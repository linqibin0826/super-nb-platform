package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// V9 迁移冒烟:四张表存在、42 条 seed 数据完整、关键唯一约束生效。
@SpringBootTest(classes = AchievementSchemaMigrationTestApp.class)
@Testcontainers
class AchievementSchemaMigrationTest {

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
    JdbcTemplate jdbc;

    @Test
    void seedsExactlyFortyTwoMvpDefinitions() {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM activity.achievement_definition WHERE first_batch_no = 1", Integer.class);
        assertThat(count).isEqualTo(42);
    }

    @Test
    void allCodesAreUnique() {
        Integer distinctCodes = jdbc.queryForObject(
                "SELECT COUNT(DISTINCT code) FROM activity.achievement_definition", Integer.class);
        assertThat(distinctCodes).isEqualTo(42);
    }

    @Test
    void nbPointsSumMatchesDeepDesignTotal() {
        Long total = jdbc.queryForObject(
                "SELECT SUM(nb_points) FROM activity.achievement_definition", Long.class);
        assertThat(total).isEqualTo(943L); // 深化稿 §2.2:MVP 42 条共 943 NB
    }

    @Test
    void achievementUnlockUniqueConstraintRejectsDuplicateCode() {
        jdbc.update("INSERT INTO activity.achievement_unlock "
                + "(id, user_id, achievement_code, unlocked_at, points_at_unlock, unlock_source) "
                + "VALUES (1, 42, 'checkin_first', now(), 5, 'batch_scan')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO activity.achievement_unlock "
                + "(id, user_id, achievement_code, unlocked_at, points_at_unlock, unlock_source) "
                + "VALUES (2, 42, 'checkin_first', now(), 5, 'batch_scan')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void userMetricAndUnlockStatAcceptNaturalKeyUpsert() {
        jdbc.update("INSERT INTO activity.user_metric (user_id, metric_code, value) VALUES (42, 'api_call_total_count', 3) "
                + "ON CONFLICT (user_id, metric_code) DO UPDATE SET value = EXCLUDED.value");
        jdbc.update("INSERT INTO activity.achievement_unlock_stat (achievement_code, unlock_count, unlock_rate) "
                + "VALUES ('checkin_first', 10, 0.5) "
                + "ON CONFLICT (achievement_code) DO UPDATE SET unlock_count = EXCLUDED.unlock_count");
        Integer v = jdbc.queryForObject(
                "SELECT value FROM activity.user_metric WHERE user_id=42 AND metric_code='api_call_total_count'",
                Integer.class);
        assertThat(v).isEqualTo(3);
    }
}
