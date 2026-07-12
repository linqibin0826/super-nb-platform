package me.supernb.activity.app.usecase.registry.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.referral.config.ReferralProperties;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.model.read.registry.RegistryEntryStatus;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import org.junit.jupiter.api.Test;

/// 注册表状态聚合:四源→四固定 id;窗口 [start,end) 判 upcoming/running/ended;
/// 无进行中 lottery/raffle 时对应条目缺席(前端无徽章降级),qq 与 leaderboard 恒在。
class RegistryStatusQueryServiceTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final RaffleCampaignPort rafflePort = mock(RaffleCampaignPort.class);
    private final Instant now = Instant.now();

    private RegistryStatusQueryService service(Instant qqStart, Instant qqEnd) {
        ReferralProperties referral = new ReferralProperties(
                qqStart.toString(), qqEnd.toString(), 77L, new BigDecimal("288"), 10);
        return new RegistryStatusQueryService(campaignPort, rafflePort, referral);
    }

    @Test
    void aggregatesAllFourSourcesWhenActive() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(new Campaign(
                1L, "幸运余额包", now.minus(1, ChronoUnit.DAYS), now.plus(10, ChronoUnit.DAYS),
                "active", new BigDecimal("1"))));
        when(rafflePort.current()).thenReturn(Optional.of(raffle("active",
                now.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS), null)));
        List<RegistryEntryStatus> out =
                service(now.minus(1, ChronoUnit.DAYS), now.plus(3, ChronoUnit.DAYS)).status();

        assertThat(out).extracting(RegistryEntryStatus::id)
                .containsExactly("lottery", "raffle", "qq-referral", "leaderboard");
        assertThat(out).extracting(RegistryEntryStatus::status)
                .containsExactly("running", "running", "running", "running");
        assertThat(out.get(3).kind()).isEqualTo("evergreen");
        assertThat(out.get(3).startsAt()).isNull();
    }

    @Test
    void lotteryAndRaffleAbsentWhenNoCurrentPeriod() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        when(rafflePort.current()).thenReturn(Optional.empty());
        List<RegistryEntryStatus> out =
                service(now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS)).status();

        assertThat(out).extracting(RegistryEntryStatus::id)
                .containsExactly("qq-referral", "leaderboard");
        assertThat(out.get(0).status()).isEqualTo("ended"); // 窗口已过,end 排他
    }

    @Test
    void raffleDrawnMapsToEndedAndUpcomingWindowsReported() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(new Campaign(
                1L, "n", now.plus(1, ChronoUnit.DAYS), now.plus(9, ChronoUnit.DAYS),
                "active", BigDecimal.ONE)));
        when(rafflePort.current()).thenReturn(Optional.of(raffle("drawn",
                now.minus(3, ChronoUnit.HOURS), now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS))));
        List<RegistryEntryStatus> out =
                service(now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS)).status();

        assertThat(out.get(0).status()).isEqualTo("upcoming"); // lottery 未开窗
        assertThat(out.get(1).status()).isEqualTo("ended");    // drawn 一律 ended(留台重放不影响徽章)
        assertThat(out.get(1).kind()).isEqualTo("raffle");
        assertThat(out.get(1).endsAt()).isEqualTo(now.minus(1, ChronoUnit.HOURS)); // endsAt=drawAt
    }

    private RaffleCampaign raffle(String status, Instant open, Instant close, Instant drawAt, Instant drawnAt) {
        return new RaffleCampaign(2L, "发布会", open, close, drawAt, GateType.RECHARGE,
                new BigDecimal("100"), open, null, WeightMode.EQUAL, status, drawnAt, null, null);
    }
}
