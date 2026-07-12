package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import me.supernb.activity.domain.model.raffle.RaffleDrawSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 开奖单事务:CAS 幂等、开奖时点实时复核(不吃报名快照)、不放回一人一件、账龄闸、留痕统计。
@SpringBootTest(classes = RaffleInfraTestApp.class)
@Testcontainers
class RaffleDrawAdapterTest {

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

    @Autowired RaffleDrawAdapter adapter;
    @Autowired RaffleEntryAdapter entryAdapter;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE activity.raffle_entry, activity.raffle_prize, activity.raffle_campaign");
        RaffleInfraTestApp.GATE_VALUES.clear();
        RaffleInfraTestApp.REGISTERED_ATS.clear();
        // 期:门槛充值满 100,draw_at 已到(1 分钟前)
        jdbc.update("INSERT INTO activity.raffle_campaign (id, name, entry_open_at, entry_close_at, draw_at, "
                + "gate_type, gate_amount, gate_from) VALUES (1, '第一届发布会', now() - interval '3 day', "
                + "now() - interval '1 hour', now() - interval '1 minute', 'RECHARGE', 100, '2026-01-01')");
        // 两件奖品,S 在前
        jdbc.update("INSERT INTO activity.raffle_prize (id, campaign_id, tier, display_name, kind, payload, sort_order) "
                + "VALUES (1001, 1, 'S', '疯狂星期四专项(V我50)', 'ALIPAY_CODE', 'FAKE-KFC-50', 0), "
                + "(1002, 1, 'C', '订阅体验专项配套', 'REDEEM_CODE', 'FAKE-SUB-CODE', 1)");
        // 三人报名;门槛桩:41/42 复核达标,43 复核跌破(报名后退款情景)
        for (long uid : new long[] {41, 42, 43}) {
            RaffleInfraTestApp.GATE_VALUES.put(uid, new BigDecimal("150"));
            entryAdapter.enter(1, uid, new BigDecimal("150"), null, null);
        }
        RaffleInfraTestApp.GATE_VALUES.put(43L, new BigDecimal("60"));
    }

    @Test
    void drawAssignsDistinctEligibleWinnersAndRecordsStats() {
        RaffleDrawSummary s = adapter.drawCampaign(1, new Random(7));
        assertThat(s.executed()).isTrue();
        assertThat(s.winners()).isEqualTo(2);       // 2 件奖品,2 名合格者(41/42)
        assertThat(s.disqualified()).isEqualTo(1);  // 43 复核跌破
        assertThat(winnerIds()).doesNotHaveDuplicates()
                .allMatch(uid -> uid == 41L || uid == 42L); // 43 绝不中奖
        Map<String, Object> c = jdbc.queryForMap(
                "SELECT status, drawn_at, entrant_count_at_draw, disqualified_count "
                        + "FROM activity.raffle_campaign WHERE id = 1");
        assertThat(c.get("status")).isEqualTo("drawn");
        assertThat(c.get("drawn_at")).isNotNull();
        assertThat(((Number) c.get("entrant_count_at_draw")).intValue()).isEqualTo(3);
        assertThat(((Number) c.get("disqualified_count")).intValue()).isEqualTo(1);
    }

    @Test
    void secondDrawIsSkippedAndWinnersUnchanged() {
        adapter.drawCampaign(1, new Random(7));
        List<Long> before = winnerIds();
        RaffleDrawSummary again = adapter.drawCampaign(1, new Random(999));
        assertThat(again.executed()).isFalse(); // CAS 未命中,无操作
        assertThat(winnerIds()).isEqualTo(before);
    }

    @Test
    void fewerEligibleThanPrizesLeavesRestUnassigned() {
        RaffleInfraTestApp.GATE_VALUES.put(41L, new BigDecimal("1")); // 只剩 42 合格
        RaffleDrawSummary s = adapter.drawCampaign(1, new Random(7));
        assertThat(s.winners()).isEqualTo(1);
        assertThat(s.disqualified()).isEqualTo(2);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM activity.raffle_prize WHERE winner_user_id IS NULL", Integer.class))
                .isEqualTo(1); // 流拍件保持无主
    }

    @Test
    void accountAgeGateDisqualifiesYoungAndUnknownAccounts() {
        jdbc.update("UPDATE activity.raffle_campaign SET min_account_age_days = 7 WHERE id = 1");
        Instant now = Instant.now();
        RaffleInfraTestApp.REGISTERED_ATS.put(41L, now.minus(Duration.ofDays(30))); // 老号
        RaffleInfraTestApp.REGISTERED_ATS.put(42L, now.minus(Duration.ofDays(1)));  // 新号,不满 7 天
        // 43 缺席注册桩 = 查无此人,同样不合格
        RaffleDrawSummary s = adapter.drawCampaign(1, new Random(7));
        assertThat(s.winners()).isEqualTo(1);
        assertThat(winnerIds()).containsExactly(41L);
    }

    @Test
    void zeroEntrantsStillTerminates() {
        jdbc.execute("TRUNCATE activity.raffle_entry");
        RaffleDrawSummary s = adapter.drawCampaign(1, new Random(7));
        assertThat(s.executed()).isTrue();
        assertThat(s.winners()).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT status FROM activity.raffle_campaign WHERE id = 1", String.class)).isEqualTo("drawn");
    }

    private List<Long> winnerIds() {
        return jdbc.queryForList("SELECT winner_user_id FROM activity.raffle_prize "
                + "WHERE winner_user_id IS NOT NULL ORDER BY id", Long.class);
    }
}
