package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;

import me.supernb.activity.domain.port.read.LeaderboardAchievementSignalPort;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 历史最佳名次 = 全部快照(跨 period/metric)取 MIN。
@Testcontainers
class LeaderboardAchievementSignalAdapterTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static LeaderboardAchievementSignalAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA activity");
        // 与 V3__leaderboard_rank_snapshot.sql 定义同步维护
        jdbc.execute("CREATE TABLE activity.leaderboard_rank_snapshot (snapshot_date DATE NOT NULL, "
                + "period TEXT NOT NULL, metric TEXT NOT NULL, user_id BIGINT NOT NULL, rank INT NOT NULL, "
                + "PRIMARY KEY (snapshot_date, period, metric, user_id))");
        adapter = new LeaderboardAchievementSignalAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.leaderboard_rank_snapshot");
    }

    @Test
    void bestRankEverPicksMinimumAcrossAllSnapshots() {
        jdbc.update(
                "INSERT INTO activity.leaderboard_rank_snapshot VALUES (DATE '2026-07-01', 'day', 'tokens', 42, 30)");
        jdbc.update(
                "INSERT INTO activity.leaderboard_rank_snapshot VALUES (DATE '2026-07-02', 'week', 'amount', 42, 5)");
        assertThat(adapter.bestRankEver()).containsExactly(new LeaderboardAchievementSignalPort.UserRank(42L, 5));
    }
}
