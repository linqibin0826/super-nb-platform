package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.RaffleGateAchievementSignalPort;
import org.junit.jupiter.api.Test;

/// 五个信号独立发布;任一信号取数异常不阻断其余四个(仿 RankSnapshotJob"逐组 try/catch"惯例)。
class RaffleGateMetricSyncJobTest {

    private final RaffleGateAchievementSignalPort signalPort = mock(RaffleGateAchievementSignalPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);

    private RaffleGateMetricSyncJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new RaffleGateMetricSyncJob(signalPort, metricPort, settlementProperties);
    }

    @Test
    void skipsWhenScanDisabled() {
        job(false).syncDaily();
        verify(signalPort, never()).raffleEntryCounts();
    }

    @Test
    void writesAllFiveMetricsIndependently() {
        when(signalPort.raffleEntryCounts())
                .thenReturn(List.of(new RaffleGateAchievementSignalPort.UserCount(1L, 1L)));
        when(signalPort.raffleWinCounts()).thenReturn(List.of());
        when(signalPort.raffleCompanionCounts()).thenReturn(List.of());
        when(signalPort.gateWinCounts()).thenReturn(List.of());
        when(signalPort.drawcardCounts())
                .thenReturn(List.of(new RaffleGateAchievementSignalPort.UserCount(2L, 3L)));

        job(true).syncDaily();

        verify(metricPort).upsertBatch("raffle_entry_count", Map.of(1L, 1.0));
        verify(metricPort).upsertBatch("drawcard_count", Map.of(2L, 3.0));
    }

    @Test
    void oneMetricFailureDoesNotBlockOthers() {
        when(signalPort.raffleEntryCounts()).thenThrow(new RuntimeException("boom"));
        when(signalPort.raffleWinCounts()).thenReturn(List.of());
        when(signalPort.raffleCompanionCounts()).thenReturn(List.of());
        when(signalPort.gateWinCounts())
                .thenReturn(List.of(new RaffleGateAchievementSignalPort.UserCount(9L, 1L)));
        when(signalPort.drawcardCounts()).thenReturn(List.of());

        job(true).syncDaily(); // 不应向上抛出

        verify(metricPort).upsertBatch("gate_win_count", Map.of(9L, 1.0));
        verify(metricPort, never()).upsertBatch(eq("raffle_entry_count"), any());
    }
}
