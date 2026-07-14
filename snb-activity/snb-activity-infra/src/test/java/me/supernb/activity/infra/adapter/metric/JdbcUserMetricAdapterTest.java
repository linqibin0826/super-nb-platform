package me.supernb.activity.infra.adapter.metric;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// user_metric 通用底座:upsert 幂等覆盖、批量写入、逐用户取全部指标、水位线候选发现。
@Testcontainers
class JdbcUserMetricAdapterTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcUserMetricAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA activity");
        // 与 V9__achievement_baseline.sql 的 user_metric 定义同步维护
        jdbc.execute("CREATE TABLE activity.user_metric (user_id BIGINT NOT NULL, metric_code TEXT NOT NULL, "
                + "value NUMERIC(20,2) NOT NULL DEFAULT 0, updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), "
                + "PRIMARY KEY (user_id, metric_code))");
        adapter = new JdbcUserMetricAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.user_metric");
    }

    @Test
    void upsertIsIdempotentAndOverwritesValue() {
        adapter.upsert(42, "api_call_total_count", 3);
        adapter.upsert(42, "api_call_total_count", 7);
        assertThat(adapter.value(42, "api_call_total_count")).contains(7.0);
    }

    @Test
    void upsertBatchWritesMultipleUsersForSameMetric() {
        adapter.upsertBatch("checkin_total_count", Map.of(1L, 5.0, 2L, 12.0));
        assertThat(adapter.value(1, "checkin_total_count")).contains(5.0);
        assertThat(adapter.value(2, "checkin_total_count")).contains(12.0);
    }

    @Test
    void allMetricsReturnsEveryCodeForUser() {
        adapter.upsert(9, "checkin_total_count", 3);
        adapter.upsert(9, "api_call_total_count", 40);
        assertThat(adapter.allMetrics(9)).containsExactlyInAnyOrderEntriesOf(
                Map.of("checkin_total_count", 3.0, "api_call_total_count", 40.0));
    }

    @Test
    void usersUpdatedSinceExcludesUntouchedRows() throws InterruptedException {
        adapter.upsert(1, "checkin_total_count", 1);
        Thread.sleep(50);
        Instant cutoff = Instant.now();
        Thread.sleep(50);
        adapter.upsert(2, "checkin_total_count", 2);
        assertThat(adapter.usersUpdatedSince(cutoff)).containsExactly(2L);
    }
}
