package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.junit.jupiter.api.Test;

/// 判定引擎:开关短路、gte/lte 两种比较符、已解锁去重、meta_combo 动态类目引用、
/// meta_regular 合成指标反映本轮解锁结果(排序无关的正确性)。
class AchievementJudgeEngineTest {

    private final UserMetricPort metricPort = mock(UserMetricPort.class);
    private final ScanWatermarkPort watermarkPort = mock(ScanWatermarkPort.class);
    private final AchievementCatalogPort catalogPort = mock(AchievementCatalogPort.class);
    private final AchievementUnlockPort unlockPort = mock(AchievementUnlockPort.class);

    private AchievementJudgeEngine engine(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new AchievementJudgeEngine(metricPort, watermarkPort, catalogPort, unlockPort, settlementProperties);
    }

    private static AchievementDefinition def(String code, String category, int nb, String predicateKind,
            String metricCode, BigDecimal threshold, String comparator, String prerequisite) {
        return new AchievementDefinition(code, null, null, category, "T1", nb, false, false, "active",
                predicateKind, metricCode, threshold, comparator, prerequisite,
                LocalDate.of(2026, 7, 13), 0, code, "flavor", null, "测试条件");
    }

    @Test
    void skipsWhenScanDisabled() {
        engine(false).judgeHourly();
        verify(metricPort, never()).usersUpdatedSince(any());
    }

    @Test
    void unlocksWhenGteThresholdMet() {
        when(watermarkPort.get("achievement_judge_engine")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(any())).thenReturn(List.of(42L));
        when(catalogPort.activeDefinitions()).thenReturn(List.of(
                def("checkin_first", "入职档案", 5, "metric_threshold", "checkin_total_count",
                        new BigDecimal("1"), "gte", null)));
        when(unlockPort.unlockedCodes(42)).thenReturn(Set.of());
        when(metricPort.allMetrics(42)).thenReturn(Map.of("checkin_total_count", 1.0));

        engine(true).judgeHourly();

        verify(unlockPort).unlock(eq(42L), eq("checkin_first"), any(), eq(5), eq("batch_scan"));
    }

    @Test
    void lteComparatorUnlocksWhenValueAtOrBelowThreshold() {
        when(watermarkPort.get("achievement_judge_engine")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(any())).thenReturn(List.of(7L));
        when(catalogPort.activeDefinitions()).thenReturn(List.of(
                def("leaderboard_1", "联动矩阵", 15, "metric_threshold", "leaderboard_best_rank_ever",
                        new BigDecimal("50"), "lte", null)));
        when(unlockPort.unlockedCodes(7)).thenReturn(Set.of());
        when(metricPort.allMetrics(7)).thenReturn(Map.of("leaderboard_best_rank_ever", 30.0));

        engine(true).judgeHourly();

        verify(unlockPort).unlock(eq(7L), eq("leaderboard_1"), any(), eq(15), eq("batch_scan"));
    }

    @Test
    void alreadyUnlockedCodeIsSkipped() {
        when(watermarkPort.get("achievement_judge_engine")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(any())).thenReturn(List.of(42L));
        when(catalogPort.activeDefinitions()).thenReturn(List.of(
                def("checkin_first", "入职档案", 5, "metric_threshold", "checkin_total_count",
                        new BigDecimal("1"), "gte", null)));
        when(unlockPort.unlockedCodes(42)).thenReturn(Set.of("checkin_first"));
        when(metricPort.allMetrics(42)).thenReturn(Map.of("checkin_total_count", 5.0));

        engine(true).judgeHourly();

        verify(unlockPort, never()).unlock(eq(42L), eq("checkin_first"), any(), anyInt(), any());
    }

    @Test
    void metaCategoryOnboardingUnlocksWhenAllCategoryMembersUnlocked() {
        when(watermarkPort.get("achievement_judge_engine")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(any())).thenReturn(List.of(9L));
        var checkinFirst = def("checkin_first", "入职档案", 5, "metric_threshold",
                "checkin_total_count", new BigDecimal("1"), "gte", null);
        var apiFirst = def("api_first_call", "入职档案", 5, "metric_threshold",
                "api_call_total_count", new BigDecimal("1"), "gte", null);
        var onboarding = def("meta_category_onboarding", "元编年史", 40, "meta_combo",
                null, null, null, "入职档案");
        when(catalogPort.activeDefinitions()).thenReturn(List.of(checkinFirst, apiFirst, onboarding));
        when(unlockPort.unlockedCodes(9)).thenReturn(Set.of("checkin_first", "api_first_call"));
        when(metricPort.allMetrics(9)).thenReturn(Map.of());

        engine(true).judgeHourly();

        verify(unlockPort).unlock(eq(9L), eq("meta_category_onboarding"), any(), eq(40), eq("batch_scan"));
    }

    @Test
    void metaRegularReflectsThisPassUnlocksRegardlessOfDefinitionOrder() {
        when(watermarkPort.get("achievement_judge_engine")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(any())).thenReturn(List.of(5L));
        var checkinFirst = def("checkin_first", "入职档案", 5, "metric_threshold",
                "checkin_total_count", new BigDecimal("1"), "gte", null);
        var metaRegular = def("meta_regular", "元编年史", 15, "metric_threshold",
                "achievement_unlock_total_count", new BigDecimal("1"), "gte", null);
        when(catalogPort.activeDefinitions()).thenReturn(List.of(checkinFirst, metaRegular));
        // 第一次查(judgeMetricThresholds 阶段开始时):尚无解锁;第二次查(judgeMetaCombos 阶段):
        // checkin_first 已在本轮解锁——验证 meta_regular 不受 activeDefinitions() 返回顺序影响
        when(unlockPort.unlockedCodes(5)).thenReturn(Set.of(), Set.of("checkin_first"));
        when(metricPort.allMetrics(5)).thenReturn(Map.of("checkin_total_count", 1.0));
        when(unlockPort.unlockedCount(5)).thenReturn(1);

        engine(true).judgeHourly();

        verify(unlockPort).unlock(eq(5L), eq("checkin_first"), any(), eq(5), eq("batch_scan"));
        verify(unlockPort).unlock(eq(5L), eq("meta_regular"), any(), eq(15), eq("batch_scan"));
    }

    @Test
    void crossSurfaceUnlocksOnlyWhenBothAxesPresent() {
        when(watermarkPort.get("achievement_judge_engine")).thenReturn(Optional.empty());
        when(metricPort.usersUpdatedSince(any())).thenReturn(List.of(11L, 12L));
        var crossSurface = def("cross_surface_user", "机房作业", 15, "metric_threshold",
                "cross_surface_flag", new BigDecimal("1"), "gte", null);
        when(catalogPort.activeDefinitions()).thenReturn(List.of(crossSurface));
        when(unlockPort.unlockedCodes(11)).thenReturn(Set.of());
        when(unlockPort.unlockedCodes(12)).thenReturn(Set.of());
        when(metricPort.allMetrics(11)).thenReturn(Map.of());
        when(metricPort.allMetrics(12)).thenReturn(Map.of());
        // 双轴齐备 → 解锁;只有单轴 → 不解锁
        when(metricPort.value(11, "api_call_total_count")).thenReturn(Optional.of(3.0));
        when(metricPort.value(11, "gallery_generate_done_count")).thenReturn(Optional.of(1.0));
        when(metricPort.value(12, "api_call_total_count")).thenReturn(Optional.of(3.0));
        when(metricPort.value(12, "gallery_generate_done_count")).thenReturn(Optional.empty());

        engine(true).judgeHourly();

        verify(unlockPort).unlock(eq(11L), eq("cross_surface_user"), any(), eq(15), eq("batch_scan"));
        verify(unlockPort, never()).unlock(eq(12L), eq("cross_surface_user"), any(), anyInt(), any());
    }
}
