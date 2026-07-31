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

    /// 🚨 返网费 ¥30 门槛必须认闲鱼购码的老客户——窗口拉到全历史,但口径仍是含
    /// 非镜像 balance 兑换码的 RECHARGE,不是只算 payment_orders 的金票老口径。
    @Test
    void lifetimeRechargeQueriesFromEpochWithRechargeGate() {
        Instant asOf = Instant.parse("2026-08-05T10:00:00Z");
        when(readModel.gateValue(42L, "RECHARGE", Instant.EPOCH, asOf)).thenReturn(new BigDecimal("30.00"));
        assertThat(adapter.lifetimeRecharge(42L, asOf)).isEqualByComparingTo("30.00");
        verify(readModel).gateValue(42L, "RECHARGE", Instant.EPOCH, asOf);
    }
}
