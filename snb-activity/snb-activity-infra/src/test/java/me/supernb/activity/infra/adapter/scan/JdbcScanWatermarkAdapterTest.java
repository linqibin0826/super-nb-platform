package me.supernb.activity.infra.adapter.scan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 水位线:未运行过返回 empty、推进后覆盖读回、不同 job_name 互不干扰。
@Testcontainers
class JdbcScanWatermarkAdapterTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcScanWatermarkAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA activity");
        // 与 Plan A V8__checkin_baseline.sql 的 checkin_scan_watermark 定义同步维护
        jdbc.execute("CREATE TABLE activity.checkin_scan_watermark (job_name TEXT PRIMARY KEY, "
                + "watermark TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL DEFAULT now())");
        adapter = new JdbcScanWatermarkAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.checkin_scan_watermark");
    }

    @Test
    void neverRunJobReturnsEmpty() {
        assertThat(adapter.get("usage_metric_sync")).isEmpty();
    }

    @Test
    void advanceThenGetReturnsSameInstant() {
        Instant t = Instant.parse("2026-07-01T00:00:00Z");
        adapter.advance("usage_metric_sync", t);
        assertThat(adapter.get("usage_metric_sync")).contains(t);
    }

    @Test
    void advanceOverwritesPreviousValueAndDifferentJobsAreIndependent() {
        adapter.advance("usage_metric_sync", Instant.parse("2026-07-01T00:00:00Z"));
        adapter.advance("usage_metric_sync", Instant.parse("2026-07-02T00:00:00Z"));
        adapter.advance("achievement_judge_engine", Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(adapter.get("usage_metric_sync")).contains(Instant.parse("2026-07-02T00:00:00Z"));
        assertThat(adapter.get("achievement_judge_engine")).contains(Instant.parse("2026-06-01T00:00:00Z"));
    }
}
