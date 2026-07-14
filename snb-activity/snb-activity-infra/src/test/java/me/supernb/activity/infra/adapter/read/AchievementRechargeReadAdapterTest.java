package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import me.supernb.sub2api.gate.GateReadModel;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.junit.jupiter.api.Test;

/// 薄委托:全量充值转发 GateReadModel(已含防双算口径,不重写);连续 3 月转发
/// RechargeReadModel 的窗口口径三连查,任一窗口 ≤0 即判 false 并短路(不必查满三次)。
class AchievementRechargeReadAdapterTest {

    private final GateReadModel gateReadModel = mock(GateReadModel.class);
    private final RechargeReadModel rechargeReadModel = mock(RechargeReadModel.class);
    private final AchievementRechargeReadAdapter adapter =
            new AchievementRechargeReadAdapter(gateReadModel, rechargeReadModel);

    @Test
    void totalRechargedDelegatesToGateReadModel() {
        when(gateReadModel.totalRecharged(42)).thenReturn(new BigDecimal("640"));
        assertThat(adapter.totalRecharged(42)).isEqualByComparingTo("640");
    }

    @Test
    void threeConsecutiveMonthsTrueWhenAllThreeWindowsPositive() {
        when(rechargeReadModel.totalRecharge(eq(42L), any(), any())).thenReturn(new BigDecimal("10"));
        assertThat(adapter.hasThreeConsecutiveMonthsOfRecharge(42)).isTrue();
        verify(rechargeReadModel, times(3)).totalRecharge(eq(42L), any(), any());
    }

    @Test
    void threeConsecutiveMonthsFalseWhenAnyWindowIsZero() {
        when(rechargeReadModel.totalRecharge(eq(7L), any(), any()))
                .thenReturn(new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("5"));
        assertThat(adapter.hasThreeConsecutiveMonthsOfRecharge(7)).isFalse();
    }
}
