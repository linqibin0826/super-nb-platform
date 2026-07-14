package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 目录只读 + 解锁写读:42 条 seed 可查、解锁幂等、已读回执只影响指定 code。
@SpringBootTest(classes = AchievementInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class AchievementCatalogAndUnlockAdapterTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/activity");
        r.add("spring.flyway.schemas", () -> "activity");
    }

    @Autowired
    AchievementCatalogAdapter catalog;

    @Autowired
    AchievementUnlockAdapter unlockAdapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.achievement_unlock");
    }

    @Test
    void activeDefinitionsReturnsAllFortyTwoSeeded() {
        assertThat(catalog.activeDefinitions()).hasSize(42);
    }

    @Test
    void byCodeFindsSeededRowWithCorrectFields() {
        var def = catalog.byCode("checkin_first").orElseThrow();
        assertThat(def.category()).isEqualTo("入职档案");
        assertThat(def.nbPoints()).isEqualTo(5);
        assertThat(def.rarity()).isEqualTo("T1");
    }

    @Test
    void unlockIsIdempotentPerUserAndCode() {
        boolean first = unlockAdapter.unlock(42, "checkin_first", Instant.now(), 5, "batch_scan");
        boolean second = unlockAdapter.unlock(42, "checkin_first", Instant.now(), 5, "batch_scan");
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(unlockAdapter.unlockedCount(42)).isEqualTo(1);
    }

    @Test
    void unlockedCodesAndMyUnlocksReflectState() {
        unlockAdapter.unlock(7, "checkin_first", Instant.now(), 5, "batch_scan");
        unlockAdapter.unlock(7, "api_first_call", Instant.now(), 5, "batch_scan");
        assertThat(unlockAdapter.unlockedCodes(7)).containsExactlyInAnyOrder("checkin_first", "api_first_call");
        assertThat(unlockAdapter.myUnlocks(7)).hasSize(2);
    }

    @Test
    void markSeenOnlyAffectsRequestedCodes() {
        unlockAdapter.unlock(9, "checkin_first", Instant.now(), 5, "batch_scan");
        unlockAdapter.unlock(9, "api_first_call", Instant.now(), 5, "batch_scan");
        int updated = unlockAdapter.markSeen(9, List.of("checkin_first"));
        assertThat(updated).isEqualTo(1);
        var unlocks = unlockAdapter.myUnlocks(9);
        assertThat(unlocks.stream().filter(u -> u.achievementCode().equals("checkin_first")).findFirst()
                .orElseThrow().seen()).isTrue();
        assertThat(unlocks.stream().filter(u -> u.achievementCode().equals("api_first_call")).findFirst()
                .orElseThrow().seen()).isFalse();
    }

    @Test
    void allDefinitionsIncludesDraftAndRetiredNotJustActive() {
        jdbc.update("INSERT INTO activity.achievement_definition "
                + "(id, code, category, rarity, nb_points, status, predicate_kind, metric_code, "
                + " threshold_value, comparator, sort_order, name, condition_text) "
                + "VALUES (999001, 'draft_probe', '情报站', 'T1', 5, 'draft', 'metric_threshold', "
                + " 'probe_metric', 1, 'gte', 999, '占位', '占位条件')");
        List<AchievementDefinition> all = catalog.allDefinitions();
        assertThat(all).extracting(AchievementDefinition::code).contains("draft_probe", "checkin_first");
        assertThat(catalog.activeDefinitions()).extracting(AchievementDefinition::code)
                .doesNotContain("draft_probe");
    }

    @Test
    void allSeriesLabelsReturnsSeededDisplayNames() {
        Map<String, String> labels = catalog.allSeriesLabels();
        assertThat(labels).containsKey("api_calls");
        assertThat(labels.get("api_calls")).contains("API CALLS");
    }
}
