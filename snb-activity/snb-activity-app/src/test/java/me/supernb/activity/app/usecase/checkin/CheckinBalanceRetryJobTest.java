package me.supernb.activity.app.usecase.checkin;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinBalanceProperties;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardRecord;
import me.supernb.activity.domain.port.checkin.BalanceGrantPort;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import org.junit.jupiter.api.Test;

/// 补偿 job:重发 pending/failed,并补录「已打卡但台账缺行」的用户
/// (后者天然覆盖新功能上线前当天已打过卡的人,不需要一次性补扫脚本)。
class CheckinBalanceRetryJobTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CheckinDailyRewardPort ledger = mock(CheckinDailyRewardPort.class);
    private final CheckinPort checkinPort = mock(CheckinPort.class);
    private final BalanceGrantPort grantPort = mock(BalanceGrantPort.class);
    private final CheckinBalanceGrantService settleService = mock(CheckinBalanceGrantService.class);
    private final CheckinBalanceProperties props = new CheckinBalanceProperties(true, "0.1", "30", "3000", 1);

    private CheckinBalanceRetryJob job() {
        return new CheckinBalanceRetryJob(ledger, checkinPort, grantPort, settleService, props);
    }

    @Test
    void retriesPendingRowAndMarksSuccess() {
        CheckinDailyRewardRecord row = new CheckinDailyRewardRecord(900L, 42L, LocalDate.of(2026, 8, 7),
                7, 21, new BigDecimal("0.70"), "pending", 0);
        when(ledger.retryable(3)).thenReturn(List.of(row));
        when(checkinPort.userIdsCheckedInOn(any())).thenReturn(List.of());

        job().run();

        verify(grantPort).grant(42L, new BigDecimal("0.70"), "checkin-daily-2026-08-07");
        verify(ledger).markSuccess(900L);
    }

    @Test
    void retryFailureMarksFailedAgain() {
        CheckinDailyRewardRecord row = new CheckinDailyRewardRecord(901L, 42L, LocalDate.of(2026, 8, 7),
                7, 21, new BigDecimal("0.70"), "failed", 1);
        when(ledger.retryable(3)).thenReturn(List.of(row));
        when(checkinPort.userIdsCheckedInOn(any())).thenReturn(List.of());
        doThrow(new RuntimeException("上游 502")).when(grantPort).grant(anyLong(), any(), anyString());

        job().run();

        verify(ledger).markFailed(eq(901L), anyString());
    }

    @Test
    void backfillsUsersWhoPunchedBeforeFeatureWentLive() {
        LocalDate today = LocalDate.now(ZONE);
        when(ledger.retryable(3)).thenReturn(List.of());
        when(checkinPort.userIdsCheckedInOn(today)).thenReturn(List.of(77L));
        when(checkinPort.userIdsCheckedInOn(today.minusDays(1))).thenReturn(List.of());
        when(ledger.findByUserAndDay(77L, today)).thenReturn(Optional.empty());
        when(checkinPort.datesInRange(eq(77L), any(), eq(today))).thenReturn(List.of(today));

        job().run();

        verify(settleService).settle(eq(77L), eq(today), eq(1), any());
    }

    @Test
    void backfillSkipsUsersAlreadyInLedger() {
        LocalDate today = LocalDate.now(ZONE);
        when(ledger.retryable(3)).thenReturn(List.of());
        when(checkinPort.userIdsCheckedInOn(today)).thenReturn(List.of(78L));
        when(checkinPort.userIdsCheckedInOn(today.minusDays(1))).thenReturn(List.of());
        when(ledger.findByUserAndDay(78L, today)).thenReturn(Optional.of(
                new CheckinDailyRewardRecord(902L, 78L, today, 1, 3, BigDecimal.ZERO, "none", 0)));

        job().run();

        verify(settleService, never()).settle(anyLong(), any(), anyInt(), any());
    }

    @Test
    void disabledSwitchSkipsEverything() {
        CheckinBalanceProperties off = new CheckinBalanceProperties(false, "0.1", "30", "3000", 1);
        new CheckinBalanceRetryJob(ledger, checkinPort, grantPort, settleService, off).run();
        verify(ledger, never()).retryable(anyInt());
    }
}
