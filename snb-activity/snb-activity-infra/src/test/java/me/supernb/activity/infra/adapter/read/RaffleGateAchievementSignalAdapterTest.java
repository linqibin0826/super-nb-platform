package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;

import me.supernb.activity.domain.port.read.RaffleGateAchievementSignalPort;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 本域(activity schema)原生 SQL:报名/中奖/陪跑(仅已开奖期次)/金票/开卡五个计数。
@Testcontainers
class RaffleGateAchievementSignalAdapterTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static RaffleGateAchievementSignalAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA activity");
        jdbc.execute("CREATE TABLE activity.raffle_campaign (id BIGINT PRIMARY KEY, status TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE activity.raffle_entry (id BIGINT PRIMARY KEY, campaign_id BIGINT NOT NULL, "
                + "user_id BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE activity.raffle_prize (id BIGINT PRIMARY KEY, campaign_id BIGINT NOT NULL, "
                + "winner_user_id BIGINT)");
        jdbc.execute("CREATE TABLE activity.gate_attempt (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "won BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE activity.draw (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL)");
        adapter = new RaffleGateAchievementSignalAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.raffle_campaign, activity.raffle_entry, activity.raffle_prize, "
                + "activity.gate_attempt, activity.draw");
    }

    @Test
    void raffleEntryAndWinCountsAggregatePerUser() {
        jdbc.update("INSERT INTO activity.raffle_campaign (id, status) VALUES (1, 'drawn')");
        jdbc.update("INSERT INTO activity.raffle_entry (id, campaign_id, user_id) VALUES (1, 1, 42)");
        jdbc.update("INSERT INTO activity.raffle_prize (id, campaign_id, winner_user_id) VALUES (1, 1, 42)");
        assertThat(adapter.raffleEntryCounts()).contains(new RaffleGateAchievementSignalPort.UserCount(42L, 1L));
        assertThat(adapter.raffleWinCounts()).contains(new RaffleGateAchievementSignalPort.UserCount(42L, 1L));
    }

    @Test
    void raffleCompanionCountsOnlyDrawnCampaignsWithoutWin() {
        jdbc.update("INSERT INTO activity.raffle_campaign (id, status) VALUES (1, 'drawn')");
        jdbc.update("INSERT INTO activity.raffle_campaign (id, status) VALUES (2, 'active')"); // 未开奖,不计
        jdbc.update("INSERT INTO activity.raffle_entry (id, campaign_id, user_id) VALUES (1, 1, 7)"); // 已开奖未中
        jdbc.update("INSERT INTO activity.raffle_entry (id, campaign_id, user_id) VALUES (2, 2, 7)"); // 未开奖,不计
        assertThat(adapter.raffleCompanionCounts())
                .containsExactly(new RaffleGateAchievementSignalPort.UserCount(7L, 1L));
    }

    @Test
    void gateWinCountsOnlyCountsWonAttempts() {
        jdbc.update("INSERT INTO activity.gate_attempt (id, user_id, won) VALUES (1, 9, true)");
        jdbc.update("INSERT INTO activity.gate_attempt (id, user_id, won) VALUES (2, 9, false)");
        assertThat(adapter.gateWinCounts()).containsExactly(new RaffleGateAchievementSignalPort.UserCount(9L, 1L));
    }

    @Test
    void drawcardCountsAggregatesDrawTable() {
        jdbc.update("INSERT INTO activity.draw (id, user_id) VALUES (1, 3)");
        jdbc.update("INSERT INTO activity.draw (id, user_id) VALUES (2, 3)");
        assertThat(adapter.drawcardCounts()).containsExactly(new RaffleGateAchievementSignalPort.UserCount(3L, 2L));
    }
}
