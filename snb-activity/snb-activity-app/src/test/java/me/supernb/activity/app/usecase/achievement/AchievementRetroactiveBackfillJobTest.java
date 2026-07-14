package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.supernb.activity.app.usecase.achievement.config.AchievementProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.junit.jupiter.api.Test;

/// 首刷:未开启不跑;已运行过(水位线存在)不重跑;解锁时 unlock_source=retroactive_backfill。
class AchievementRetroactiveBackfillJobTest {

    private final UserMetricPort metricPort = mock(UserMetricPort.class);
    private final AchievementCatalogPort catalogPort = mock(AchievementCatalogPort.class);
    private final AchievementUnlockPort unlockPort = mock(AchievementUnlockPort.class);
    private final ScanWatermarkPort watermarkPort = mock(ScanWatermarkPort.class);
    private final AchievementJudgeEngine judgeEngine = new AchievementJudgeEngine(
            metricPort, watermarkPort, catalogPort, unlockPort,
            new CheckinSettlementProperties(new BigDecimal("250"), new BigDecimal("10"), true, false));

    private AchievementRetroactiveBackfillJob job(boolean enabled) {
        return new AchievementRetroactiveBackfillJob(metricPort, catalogPort, watermarkPort, judgeEngine,
                new AchievementProperties(enabled));
    }

    @Test
    void skipsWhenDisabled() {
        job(false).runOnce();
        verify(watermarkPort, never()).get("achievement_retroactive_backfill");
    }

    @Test
    void skipsWhenAlreadyRunBefore() {
        when(watermarkPort.get("achievement_retroactive_backfill")).thenReturn(Optional.of(Instant.now()));
        job(true).runOnce();
        verify(metricPort, never()).usersUpdatedSince(any());
    }

    @Test
    void unlocksWithRetroactiveSourceForAllHistoricalUsers() {
        when(watermarkPort.get("achievement_retroactive_backfill")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(Instant.EPOCH)).thenReturn(List.of(42L));
        when(catalogPort.activeDefinitions()).thenReturn(List.of(new AchievementDefinition(
                "checkin_first", null, null, "入职档案", "T1", 5, false, false, "active",
                "metric_threshold", "checkin_total_count", new BigDecimal("1"), "gte", null,
                LocalDate.of(2026, 7, 13), 0, "开机自检", "f", null, "首次签到")));
        when(unlockPort.unlockedCodes(42)).thenReturn(Set.of());
        when(metricPort.allMetrics(42)).thenReturn(Map.of("checkin_total_count", 3.0));

        job(true).runOnce();

        verify(unlockPort).unlock(eq(42L), eq("checkin_first"), any(), eq(5), eq("retroactive_backfill"));
        verify(watermarkPort).advance(eq("achievement_retroactive_backfill"), any());
    }
}
