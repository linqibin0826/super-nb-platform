package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.model.school.SchoolCardRecord;
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

/// 包机邀请卡:开卡幂等(user_id 唯一)、升档回写、重置银行原子扣减(used<earned 谓词)与回补。
@SpringBootTest(classes = SchoolCardInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SchoolCardAdapterTest {

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
    SchoolCardAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM activity.school_card");
    }

    @Test
    void insertThenFindRoundTrips() {
        Optional<SchoolCardRecord> inserted = adapter.insert(42L, 1, 700L);
        assertThat(inserted).isPresent();
        SchoolCardRecord found = adapter.find(42L).orElseThrow();
        assertThat(found.tier()).isEqualTo(1);
        assertThat(found.subscriptionId()).isEqualTo(700L);
        assertThat(found.resetsUsed()).isZero();
    }

    @Test
    void duplicateInsertFallsToEmpty() {
        adapter.insert(42L, 1, 700L);
        assertThat(adapter.insert(42L, 1, 701L)).isEmpty();   // 并发开卡撞唯一约束
        assertThat(adapter.find(42L).orElseThrow().subscriptionId()).isEqualTo(700L);
    }

    @Test
    void upgradeRewritesTierAndSubscription() {
        long id = adapter.insert(42L, 1, 700L).orElseThrow().id();
        adapter.upgrade(id, 2, 701L);
        SchoolCardRecord found = adapter.find(42L).orElseThrow();
        assertThat(found.tier()).isEqualTo(2);
        assertThat(found.subscriptionId()).isEqualTo(701L);
    }

    @Test
    void consumeResetStopsAtEarnedCeiling() {
        long id = adapter.insert(42L, 1, 700L).orElseThrow().id();
        assertThat(adapter.consumeReset(id, 2)).isTrue();    // used 0→1
        assertThat(adapter.consumeReset(id, 2)).isTrue();    // used 1→2
        assertThat(adapter.consumeReset(id, 2)).isFalse();   // 2≥2:扣不动
        assertThat(adapter.find(42L).orElseThrow().resetsUsed()).isEqualTo(2);
    }

    @Test
    void refundRestoresCreditAndFloorsAtZero() {
        long id = adapter.insert(42L, 1, 700L).orElseThrow().id();
        adapter.consumeReset(id, 5);
        adapter.refundReset(id);
        assertThat(adapter.find(42L).orElseThrow().resetsUsed()).isZero();
        adapter.refundReset(id);   // 已 0 再回补不打穿到负数
        assertThat(adapter.find(42L).orElseThrow().resetsUsed()).isZero();
    }
}
