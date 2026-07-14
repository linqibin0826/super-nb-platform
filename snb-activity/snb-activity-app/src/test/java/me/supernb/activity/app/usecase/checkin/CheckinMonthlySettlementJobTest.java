package me.supernb.activity.app.usecase.checkin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinRewardCandidate;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.checkin.CheckinRewardPort;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.junit.jupiter.api.Test;

/// 月度结算 job:开关短路、满勤+充值双达标才占位发放、预算硬顶转 deferred、
/// 遗留 pending/failed(attempts<3)优先重试、传输层连续失败转告警且不向上抛异常。
class CheckinMonthlySettlementJobTest {

    private final CheckinPort checkinPort = mock(CheckinPort.class);
    private final CheckinRechargeReadPort rechargePort = mock(CheckinRechargeReadPort.class);
    private final CheckinRewardPort rewardPort = mock(CheckinRewardPort.class);
    private final SubscriptionGrantPort grantPort = mock(SubscriptionGrantPort.class);
    private final CheckinProperties props = new CheckinProperties("2020-01-01");
    private final CheckinTierProperties tierProps = new CheckinTierProperties(
            new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("500"),
            27L, 65L, 71L, new BigDecimal("0.9"), new BigDecimal("1.9"), new BigDecimal("4.4"));

    private CheckinMonthlySettlementJob job(boolean scanEnabled, boolean tierRewardEnabled, BigDecimal monthlyCap) {
        CheckinSettlementProperties settlementProps =
                new CheckinSettlementProperties(monthlyCap, new BigDecimal("10"), scanEnabled, tierRewardEnabled);
        return new CheckinMonthlySettlementJob(checkinPort, rechargePort, rewardPort, grantPort,
                props, tierProps, settlementProps);
    }

    @Test
    void skipsEntirelyWhenScanDisabled() {
        job(false, true, new BigDecimal("250")).settlePreviousMonth();
        verify(checkinPort, never()).fullAttendanceUserIds(any(), any(), anyLong());
    }

    @Test
    void skipsEntirelyWhenTierRewardDisabled() {
        job(true, false, new BigDecimal("250")).settlePreviousMonth();
        verify(checkinPort, never()).fullAttendanceUserIds(any(), any(), anyLong());
    }

    @Test
    void fullAttendanceAndQualifyingRechargeClaimsAndSendsSuccessfully() {
        when(rewardPort.byStatus("pending")).thenReturn(List.of());
        when(rewardPort.byStatus("failed")).thenReturn(List.of());
        when(checkinPort.fullAttendanceUserIds(any(), any(), anyLong())).thenReturn(List.of(42L));
        when(rechargePort.monthlyRecharges(eq(List.of(42L)), any(), any()))
                .thenReturn(Map.of(42L, new BigDecimal("55")));
        when(rewardPort.claim(eq(42L), any(), eq("B"), eq(65L), any())).thenReturn(Optional.of(9001L));
        when(grantPort.bulkGrant(eq(List.of(42L)), eq(65L), eq(3), any()))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(42L, "created"), List.of()));

        job(true, true, new BigDecimal("250")).settlePreviousMonth();

        verify(rewardPort).markSuccess(9001L);
    }

    @Test
    void overBudgetCandidateIsDeferredNotSent() {
        when(rewardPort.byStatus("pending")).thenReturn(List.of());
        when(rewardPort.byStatus("failed")).thenReturn(List.of());
        when(checkinPort.fullAttendanceUserIds(any(), any(), anyLong())).thenReturn(List.of(1L, 2L));
        when(rechargePort.monthlyRecharges(eq(List.of(1L, 2L)), any(), any()))
                .thenReturn(Map.of(1L, new BigDecimal("500"), 2L, new BigDecimal("500"))); // 均命中 C 档,¥4.4/人
        when(rewardPort.claim(eq(1L), any(), eq("C"), eq(71L), any())).thenReturn(Optional.of(1001L));
        when(rewardPort.claim(eq(2L), any(), eq("C"), eq(71L), any())).thenReturn(Optional.of(1002L));
        when(grantPort.bulkGrant(eq(List.of(1L)), eq(71L), eq(7), any()))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "created"), List.of()));

        job(true, true, new BigDecimal("5")).settlePreviousMonth(); // 预算只够一人(¥4.4),第二人会超顶

        verify(rewardPort).markSuccess(1001L);
        verify(rewardPort).markDeferred(1002L);
        verify(grantPort, times(1)).bulkGrant(anyList(), anyLong(), anyInt(), any());
    }

    @Test
    void leftoverFailedRowWithRetriesRemainingIsRetriedAndSucceeds() {
        var leftover = new CheckinRewardCandidate(2001L, 7L, LocalDate.of(2026, 6, 1), "A", 27L,
                "checkin-reward-2026-06", 1);
        when(rewardPort.byStatus("pending")).thenReturn(List.of());
        when(rewardPort.byStatus("failed")).thenReturn(List.of(leftover));
        when(checkinPort.fullAttendanceUserIds(any(), any(), anyLong())).thenReturn(List.of());
        when(grantPort.bulkGrant(eq(List.of(7L)), eq(27L), eq(3), eq("checkin-reward-2026-06")))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(7L, "created"), List.of()));

        job(true, true, new BigDecimal("250")).settlePreviousMonth();

        verify(rewardPort).markSuccess(2001L);
    }

    @Test
    void leftoverFailedRowAtRetryLimitIsExcludedFromRetry() {
        var exhausted = new CheckinRewardCandidate(2002L, 8L, LocalDate.of(2026, 6, 1), "A", 27L,
                "checkin-reward-2026-06", 3);
        when(rewardPort.byStatus("pending")).thenReturn(List.of());
        when(rewardPort.byStatus("failed")).thenReturn(List.of(exhausted));
        when(checkinPort.fullAttendanceUserIds(any(), any(), anyLong())).thenReturn(List.of());

        job(true, true, new BigDecimal("250")).settlePreviousMonth();

        verify(grantPort, never()).bulkGrant(anyList(), anyLong(), anyInt(), any());
    }

    @Test
    void transportFailureAfterThreeRetriesMarksFailedAndDoesNotThrow() {
        when(rewardPort.byStatus("pending")).thenReturn(List.of());
        when(rewardPort.byStatus("failed")).thenReturn(List.of());
        when(checkinPort.fullAttendanceUserIds(any(), any(), anyLong())).thenReturn(List.of(9L));
        when(rechargePort.monthlyRecharges(eq(List.of(9L)), any(), any()))
                .thenReturn(Map.of(9L, new BigDecimal("30")));
        when(rewardPort.claim(eq(9L), any(), eq("A"), eq(27L), any())).thenReturn(Optional.of(3001L));
        when(grantPort.bulkGrant(eq(List.of(9L)), eq(27L), eq(3), any()))
                .thenThrow(new RuntimeException("network down"));

        job(true, true, new BigDecimal("250")).settlePreviousMonth(); // 不应向上抛出

        verify(grantPort, times(3)).bulkGrant(eq(List.of(9L)), eq(27L), eq(3), any());
        verify(rewardPort).markFailed(eq(3001L), contains("transport failure"));
    }

    /// grantPort 未装配(理论不应发生——tierRewardEnabled 硬开关本应与 sub2api.admin-key 配置
    /// 同步打开,job 构造器内的 null 检查只是最后一道防线)时安全跳过:仅记错误日志后直接
    /// return,不触碰 checkinPort/rechargePort/rewardPort/grantPort 任何一个端口方法——这是
    /// 无 admin-key 环境下 job 不崩溃的最后闸门。
    @Test
    void skipsEntirelyWhenGrantPortUnavailable() {
        CheckinSettlementProperties settlementProps =
                new CheckinSettlementProperties(new BigDecimal("250"), new BigDecimal("10"), true, true);
        CheckinMonthlySettlementJob jobWithoutGrantPort = new CheckinMonthlySettlementJob(
                checkinPort, rechargePort, rewardPort, (SubscriptionGrantPort) null, props, tierProps,
                settlementProps);

        jobWithoutGrantPort.settlePreviousMonth();

        verify(checkinPort, never()).fullAttendanceUserIds(any(), any(), anyLong());
        verify(rechargePort, never()).monthlyRecharges(any(), any(), any());
        verify(rewardPort, never()).byStatus(any());
        verify(grantPort, never()).bulkGrant(any(), anyLong(), anyInt(), any());
    }
}
