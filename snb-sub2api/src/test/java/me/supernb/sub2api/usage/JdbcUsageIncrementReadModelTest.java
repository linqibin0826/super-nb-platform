package me.supernb.sub2api.usage;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 增量计数窗口精确、深夜旗标按 Asia/Shanghai 1-4 点判定、日终峰值现查当天。
@Testcontainers
class JdbcUsageIncrementReadModelTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcUsageIncrementReadModel model;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE TABLE usage_logs (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "created_at TIMESTAMPTZ NOT NULL)");
        model = new JdbcUsageIncrementReadModel(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE usage_logs");
    }

    @Test
    void callCountsSinceOnlyCountsRowsInHalfOpenWindow() {
        jdbc.update("INSERT INTO usage_logs VALUES (1, 42, ?)", Timestamp.from(Instant.parse("2026-07-13T01:00:00Z")));
        jdbc.update("INSERT INTO usage_logs VALUES (2, 42, ?)", Timestamp.from(Instant.parse("2026-07-13T02:00:00Z")));
        jdbc.update("INSERT INTO usage_logs VALUES (3, 42, ?)", Timestamp.from(Instant.parse("2026-07-13T03:00:00Z")));
        var counts = model.callCountsSince(Instant.parse("2026-07-13T01:00:00Z"), Instant.parse("2026-07-13T03:00:00Z"));
        assertThat(counts).containsEntry(42L, 2L); // [01:00,03:00) 只含 01:00 与 02:00 两行
    }

    @Test
    void lateNightFlagsDetectShanghaiOneToFourAm() {
        // 2026-07-13 02:00 上海时间 = 2026-07-12T18:00:00Z
        jdbc.update("INSERT INTO usage_logs VALUES (1, 7, ?)", Timestamp.from(Instant.parse("2026-07-12T18:00:00Z")));
        // 2026-07-13 10:00 上海时间(非深夜)
        jdbc.update("INSERT INTO usage_logs VALUES (2, 8, ?)", Timestamp.from(Instant.parse("2026-07-13T02:00:00Z")));
        var flags = model.lateNightFlagsSince(Instant.parse("2026-07-12T00:00:00Z"), Instant.parse("2026-07-14T00:00:00Z"));
        assertThat(flags).containsEntry(7L, true);
        assertThat(flags).doesNotContainKey(8L);
    }

    @Test
    void callCountsOnDayAggregatesShanghaiNaturalDay() {
        jdbc.update("INSERT INTO usage_logs VALUES (1, 3, ?)", Timestamp.from(Instant.parse("2026-07-12T16:30:00Z"))); // 07-13 00:30 上海
        jdbc.update("INSERT INTO usage_logs VALUES (2, 3, ?)", Timestamp.from(Instant.parse("2026-07-13T10:00:00Z"))); // 07-13 18:00 上海
        var counts = model.callCountsOnDay(LocalDate.of(2026, 7, 13), ZoneId.of("Asia/Shanghai"));
        assertThat(counts).containsEntry(3L, 2L);
    }
}
