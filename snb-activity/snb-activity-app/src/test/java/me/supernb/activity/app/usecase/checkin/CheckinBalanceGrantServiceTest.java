package me.supernb.activity.app.usecase.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinBalanceProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.domain.port.checkin.BalanceGrantPort;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/// 返网费结算:总闸/¥30 门槛/月度预算硬顶三道判定,不发钱也落 none 行,
/// 任何异常都不得外抛(绝不影响打卡成功)。
class CheckinBalanceGrantServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 8, 7);
    private static final Instant NOW = Instant.parse("2026-08-07T02:00:00Z");

    private final CheckinDailyRewardPort ledger = mock(CheckinDailyRewardPort.class);
    private final CheckinRechargeReadPort recharge = mock(CheckinRechargeReadPort.class);
    private final BalanceGrantPort grantPort = mock(BalanceGrantPort.class);
    private final CheckinBalanceProperties balanceProps =
            new CheckinBalanceProperties(true, "0.1", "30", "3000");
    private final CheckinProperties props = new CheckinProperties("2026-07-15", 3);

    private CheckinBalanceGrantService service() {
        return new CheckinBalanceGrantService(ledger, recharge, grantPort, balanceProps, props);
    }

    @Test
    void eligibleUserGetsBalanceAndSuccessMark() {
        when(recharge.lifetimeRecharge(42L, NOW)).thenReturn(new BigDecimal("30"));
        when(ledger.monthlyBalanceTotal(any(), any())).thenReturn(BigDecimal.ZERO);
        when(ledger.claim(eq(42L), eq(DAY), eq(7), eq(21), any(), eq("pending"), anyString()))
                .thenReturn(Optional.of(900L));

        service().settle(42L, DAY, 7, NOW);

        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(grantPort).grant(eq(42L), amount.capture(), eq("checkin-daily-2026-08-07"));
        assertThat(amount.getValue()).isEqualByComparingTo("0.70");
        verify(ledger).markSuccess(900L);
    }

    @Test
    void belowThresholdWritesNoneRowAndNeverCallsUpstream() {
        when(recharge.lifetimeRecharge(42L, NOW)).thenReturn(new BigDecimal("29.99"));
        when(ledger.claim(eq(42L), eq(DAY), eq(7), eq(21), any(), eq("none"), anyString()))
                .thenReturn(Optional.of(901L));

        service().settle(42L, DAY, 7, NOW);

        verify(grantPort, never()).grant(anyLong(), any(), anyString());
        verify(ledger).claim(eq(42L), eq(DAY), eq(7), eq(21), eq(BigDecimal.ZERO), eq("none"), anyString());
    }

    @Test
    void budgetCapExhaustedStopsBalanceButStillWritesNoneRow() {
        when(recharge.lifetimeRecharge(42L, NOW)).thenReturn(new BigDecimal("500"));
        when(ledger.monthlyBalanceTotal(any(), any())).thenReturn(new BigDecimal("3000"));
        when(ledger.claim(eq(42L), eq(DAY), eq(7), eq(21), any(), eq("none"), anyString()))
                .thenReturn(Optional.of(902L));

        service().settle(42L, DAY, 7, NOW);

        verify(grantPort, never()).grant(anyLong(), any(), anyString());
        verify(ledger).claim(eq(42L), eq(DAY), eq(7), eq(21), eq(BigDecimal.ZERO), eq("none"), anyString());
    }

    @Test
    void upstreamFailureMarksFailedAndNeverThrows() {
        when(recharge.lifetimeRecharge(42L, NOW)).thenReturn(new BigDecimal("100"));
        when(ledger.monthlyBalanceTotal(any(), any())).thenReturn(BigDecimal.ZERO);
        when(ledger.claim(anyLong(), any(), anyInt(), anyInt(), any(), eq("pending"), anyString()))
                .thenReturn(Optional.of(903L));
        doThrow(new RuntimeException("上游 502")).when(grantPort).grant(anyLong(), any(), anyString());

        service().settle(42L, DAY, 7, NOW);   // 不得抛出

        verify(ledger).markFailed(eq(903L), anyString());
    }

    @Test
    void alreadyClaimedDayDoesNothing() {
        when(recharge.lifetimeRecharge(42L, NOW)).thenReturn(new BigDecimal("100"));
        when(ledger.monthlyBalanceTotal(any(), any())).thenReturn(BigDecimal.ZERO);
        when(ledger.claim(anyLong(), any(), anyInt(), anyInt(), any(), anyString(), anyString()))
                .thenReturn(Optional.empty());   // 台账已有行

        service().settle(42L, DAY, 7, NOW);

        verify(grantPort, never()).grant(anyLong(), any(), anyString());
    }

    @Test
    void disabledTotalSwitchWritesNoneRowAndSkipsEverything() {
        CheckinBalanceProperties off = new CheckinBalanceProperties(false, "0.1", "30", "3000");
        when(ledger.claim(eq(42L), eq(DAY), eq(7), eq(21), eq(BigDecimal.ZERO), eq("none"), anyString()))
                .thenReturn(Optional.of(904L));

        new CheckinBalanceGrantService(ledger, recharge, grantPort, off, props).settle(42L, DAY, 7, NOW);

        verify(grantPort, never()).grant(anyLong(), any(), anyString());
        verify(recharge, never()).lifetimeRecharge(anyLong(), any());   // 总闸关时连门槛都不查
    }
}
