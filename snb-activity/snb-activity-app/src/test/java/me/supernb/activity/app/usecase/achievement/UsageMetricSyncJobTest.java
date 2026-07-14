package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.UsageMetricSignalPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.junit.jupiter.api.Test;

/// 小时增量:开关短路;计数累加不重叠、深夜旗标只写 true 条目;日终峰值只在超过既有值时更新。
class UsageMetricSyncJobTest {

    private final UsageMetricSignalPort signalPort = mock(UsageMetricSignalPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);
    private final ScanWatermarkPort watermarkPort = mock(ScanWatermarkPort.class);

    private UsageMetricSyncJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new UsageMetricSyncJob(signalPort, metricPort, watermarkPort, settlementProperties);
    }

    @Test
    void syncHourlySkipsWhenScanDisabled() {
        job(false).syncHourly();
        verify(watermarkPort, never()).get(any());
    }

    @Test
    void syncHourlyAccumulatesDeltaOntoExistingValue() {
        when(watermarkPort.get("usage_metric_sync"))
                .thenReturn(Optional.of(Instant.parse("2026-07-13T00:00:00Z")));
        when(signalPort.callCountsSince(eq(Instant.parse("2026-07-13T00:00:00Z")), any()))
                .thenReturn(Map.of(42L, 5L));
        when(metricPort.value(42, "api_call_total_count")).thenReturn(Optional.of(10.0));
        when(signalPort.lateNightFlagsSince(any(), any())).thenReturn(Map.of());

        job(true).syncHourly();

        verify(metricPort).upsert(42, "api_call_total_count", 15.0);
        verify(watermarkPort).advance(eq("usage_metric_sync"), any());
    }

    @Test
    void syncHourlyWritesLateNightFlagOnlyForTrueEntries() {
        when(watermarkPort.get("usage_metric_sync"))
                .thenReturn(Optional.of(Instant.parse("2026-07-13T00:00:00Z")));
        when(signalPort.callCountsSince(any(), any())).thenReturn(Map.of());
        when(signalPort.lateNightFlagsSince(any(), any())).thenReturn(Map.of(7L, true, 8L, false));

        job(true).syncHourly();

        verify(metricPort).upsert(7, "api_call_late_night_flag", 1);
        verify(metricPort, never()).upsert(eq(8L), eq("api_call_late_night_flag"), anyDouble());
    }

    @Test
    void syncDailyPeakOnlyUpdatesWhenTodayExceedsStoredPeak() {
        when(signalPort.callCountsOnDay(any(), any())).thenReturn(Map.of(42L, 120L));
        when(metricPort.value(42, "api_call_daily_peak_max")).thenReturn(Optional.of(80.0));
        job(true).syncDailyPeak();
        verify(metricPort).upsert(42, "api_call_daily_peak_max", 120L);
    }

    @Test
    void syncDailyPeakDoesNotOverwriteHigherExistingPeak() {
        when(signalPort.callCountsOnDay(any(), any())).thenReturn(Map.of(42L, 50L));
        when(metricPort.value(42, "api_call_daily_peak_max")).thenReturn(Optional.of(80.0));
        job(true).syncDailyPeak();
        verify(metricPort, never()).upsert(eq(42L), eq("api_call_daily_peak_max"), anyDouble());
    }
}
