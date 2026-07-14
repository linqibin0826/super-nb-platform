package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.junit.jupiter.api.Test;

/// 薄委托:全量充值与连续 3 月均转发既有 RaffleGateReadModel 的 RECHARGE 全口径
/// (payment_orders UNION 已核销 redeem_codes,剔 ZPay 镜像码防双算,与 A-7
/// CheckinRechargeReadPort 同款,不重写这段易错 SQL)。全量充值窗口=[EPOCH, now);
/// 连续 3 月按月窗口三连查,任一窗口 ≤0 即判 false 并短路(不必查满三次)。
class AchievementRechargeReadAdapterTest {

    private final RaffleGateReadModel raffleGateReadModel = mock(RaffleGateReadModel.class);
    private final RechargeReadModel rechargeReadModel = mock(RechargeReadModel.class);
    private final AchievementRechargeReadAdapter adapter =
            new AchievementRechargeReadAdapter(raffleGateReadModel, rechargeReadModel);

    @Test
    void totalRechargedDelegatesToRaffleGateReadModelWithEpochWindow() {
        when(raffleGateReadModel.gateValue(eq(42L), eq("RECHARGE"), eq(Instant.EPOCH), any()))
                .thenReturn(new BigDecimal("640"));
        assertThat(adapter.totalRecharged(42)).isEqualByComparingTo("640");
    }

    @Test
    void threeConsecutiveMonthsTrueWhenAllThreeWindowsPositive() {
        when(raffleGateReadModel.gateValue(eq(42L), eq("RECHARGE"), any(), any()))
                .thenReturn(new BigDecimal("10"));
        assertThat(adapter.hasThreeConsecutiveMonthsOfRecharge(42)).isTrue();
        verify(raffleGateReadModel, times(3)).gateValue(eq(42L), eq("RECHARGE"), any(), any());
    }

    @Test
    void threeConsecutiveMonthsFalseWhenAnyWindowIsZero() {
        when(raffleGateReadModel.gateValue(eq(7L), eq("RECHARGE"), any(), any()))
                .thenReturn(new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("5"));
        assertThat(adapter.hasThreeConsecutiveMonthsOfRecharge(7)).isFalse();
    }

    @Test
    void usersWithNewRechargeSinceDelegatesToRechargeReadModel() {
        Instant since = Instant.parse("2026-07-12T00:00:00Z");
        Instant until = Instant.parse("2026-07-14T00:00:00Z");
        when(rechargeReadModel.usersWithNewRechargeSince(since, until)).thenReturn(List.of(42L));
        assertThat(adapter.usersWithNewRechargeSince(since, until)).containsExactly(42L);
    }
}
