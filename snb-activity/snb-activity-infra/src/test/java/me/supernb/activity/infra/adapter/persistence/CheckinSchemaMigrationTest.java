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

/// V8 迁移冒烟:四张表存在、关键唯一约束真实生效。不装配任何被测 Bean,只验证 schema 本身
/// (家族里没有专门的"迁移测试"先例,此测试用最小 Spring 上下文触发 Flyway 后直接拿 JdbcTemplate 断言)。
@SpringBootTest(classes = CheckinSchemaMigrationTestApp.class)
@Testcontainers
class CheckinSchemaMigrationTest {

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
    void checkinRecordUniqueConstraintRejectsDuplicateDay() {
        jdbc.update("INSERT INTO activity.checkin_record (id, user_id, checkin_date, checked_in_at) "
                + "VALUES (1, 42, '2026-07-13', now())");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO activity.checkin_record "
                + "(id, user_id, checkin_date, checked_in_at) VALUES (2, 42, '2026-07-13', now())"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkinRewardGrantUniqueConstraintIsOnUserAndMonth() {
        jdbc.update("INSERT INTO activity.checkin_reward_grant "
                + "(id, user_id, grant_month, tier, group_id, status, notes) "
                + "VALUES (1, 42, '2026-07-01', 'A', 100, 'pending', 'fixed-notes')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO activity.checkin_reward_grant "
                + "(id, user_id, grant_month, tier, group_id, status, notes) "
                + "VALUES (2, 42, '2026-07-01', 'B', 101, 'pending', 'fixed-notes')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void usageSnapshotAndWatermarkTablesAcceptNaturalKeyUpsert() {
        jdbc.update("INSERT INTO activity.checkin_usage_daily_snapshot (user_id, snapshot_date, usage_count) "
                + "VALUES (42, '2026-07-13', 3) "
                + "ON CONFLICT (user_id, snapshot_date) DO UPDATE SET usage_count = EXCLUDED.usage_count");
        jdbc.update("INSERT INTO activity.checkin_scan_watermark (job_name, watermark) "
                + "VALUES ('usage_metric_sync', now()) "
                + "ON CONFLICT (job_name) DO UPDATE SET watermark = EXCLUDED.watermark");
        Integer count = jdbc.queryForObject(
                "SELECT usage_count FROM activity.checkin_usage_daily_snapshot "
                        + "WHERE user_id = 42 AND snapshot_date = '2026-07-13'", Integer.class);
        assertThat(count).isEqualTo(3);
    }
}
