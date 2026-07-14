package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import me.supernb.sub2api.raffle.RaffleGateReadModel;
import org.junit.jupiter.api.Test;

/// 薄委托:窗口内真实充值转发既有 RaffleGateReadModel 的 RECHARGE 口径(v0.1.10 已修复,不重写)。
class CheckinRechargeReadAdapterTest {

    private final RaffleGateReadModel readModel = mock(RaffleGateReadModel.class);
    private final CheckinRechargeReadAdapter adapter = new CheckinRechargeReadAdapter(readModel);

    private static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    @Test
    void singleUserDelegatesWithRechargeGateType() {
        when(readModel.gateValue(42L, "RECHARGE", START, END)).thenReturn(new BigDecimal("55.00"));
        assertThat(adapter.monthlyRecharge(42, START, END)).isEqualByComparingTo("55.00");
        verify(readModel).gateValue(42L, "RECHARGE", START, END);
    }

    @Test
    void batchDelegatesWithRechargeGateType() {
        when(readModel.gateValues(List.of(1L, 2L), "RECHARGE", START, END))
                .thenReturn(Map.of(1L, new BigDecimal("30.00")));
        assertThat(adapter.monthlyRecharges(List.of(1L, 2L), START, END))
                .containsExactly(Map.entry(1L, new BigDecimal("30.00")));
    }
}
