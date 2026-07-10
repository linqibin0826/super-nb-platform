package me.supernb.activity.infra.adapter.snapshot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcRankSnapshotAdapterTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcRankSnapshotAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA activity");
        // 与 V3__leaderboard_rank_snapshot.sql 同步维护
        jdbc.execute("CREATE TABLE activity.leaderboard_rank_snapshot ("
                + "snapshot_date DATE NOT NULL, period TEXT NOT NULL, metric TEXT NOT NULL, "
                + "user_id BIGINT NOT NULL, rank INT NOT NULL, "
                + "PRIMARY KEY (snapshot_date, period, metric, user_id))");
        adapter = new JdbcRankSnapshotAdapter(jdbc);
    }

    @Test
    void saveIsIdempotentPerDay() {
        LocalDate d = LocalDate.of(2026, 7, 10);
        adapter.save(d, BoardPeriod.WEEK, BoardMetric.TOKENS, Map.of(1L, 3, 2L, 1));
        adapter.save(d, BoardPeriod.WEEK, BoardMetric.TOKENS, Map.of(1L, 2, 2L, 1)); // 重跑覆盖
        assertThat(adapter.latestBefore(d.plusDays(1), BoardPeriod.WEEK, BoardMetric.TOKENS))
                .containsExactlyInAnyOrderEntriesOf(Map.of(1L, 2, 2L, 1));
    }

    @Test
    void latestBeforePicksMostRecentStrictlyEarlierDate() {
        adapter.save(LocalDate.of(2026, 7, 8), BoardPeriod.DAY, BoardMetric.AMOUNT, Map.of(1L, 9));
        adapter.save(LocalDate.of(2026, 7, 9), BoardPeriod.DAY, BoardMetric.AMOUNT, Map.of(1L, 5));
        assertThat(adapter.latestBefore(LocalDate.of(2026, 7, 10), BoardPeriod.DAY, BoardMetric.AMOUNT))
                .containsEntry(1L, 5);
        assertThat(adapter.latestBefore(LocalDate.of(2026, 7, 9), BoardPeriod.DAY, BoardMetric.AMOUNT))
                .containsEntry(1L, 9);   // 严格早于 → 只见 07-08
        assertThat(adapter.latestBefore(LocalDate.of(2026, 7, 8), BoardPeriod.DAY, BoardMetric.AMOUNT))
                .isEmpty();
    }

    @Test
    void purgeDropsRowsOlderThanCutoff() {
        adapter.save(LocalDate.of(2026, 6, 1), BoardPeriod.ALL, BoardMetric.TOKENS, Map.of(7L, 1));
        adapter.purgeOlderThan(LocalDate.of(2026, 6, 10));
        assertThat(adapter.latestBefore(LocalDate.of(2026, 7, 1), BoardPeriod.ALL, BoardMetric.TOKENS)).isEmpty();
    }
}
