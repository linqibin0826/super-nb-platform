package me.supernb.activity.app.usecase.achievement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.read.AchievementRechargeReadPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.junit.jupiter.api.Test;

/// 直判:普通金额档位比总额,连续 3 月档位走专属检查,两者互不干扰。
class RechargeAchievementJudgeJobTest {

    private final AchievementRechargeReadPort rechargePort = mock(AchievementRechargeReadPort.class);
    private final AchievementCatalogPort catalogPort = mock(AchievementCatalogPort.class);
    private final AchievementUnlockPort unlockPort = mock(AchievementUnlockPort.class);
    private final ScanWatermarkPort watermarkPort = mock(ScanWatermarkPort.class);

    private RechargeAchievementJudgeJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new RechargeAchievementJudgeJob(rechargePort, catalogPort, unlockPort, watermarkPort,
                settlementProperties);
    }

    private static AchievementDefinition def(String code, String metricCode, BigDecimal threshold) {
        return new AchievementDefinition(code, null, null, "补给记录", "T1", 5, false, true, "active",
                "metric_threshold", metricCode, threshold, "gte", null,
                LocalDate.of(2026, 7, 13), 0, code, "f", null, "测试条件");
    }

    @Test
    void skipsWhenScanDisabled() {
        job(false).judgeDaily();
        verify(rechargePort, never()).usersWithNewRechargeSince(any(), any());
    }

    @Test
    void unlocksAmountAchievementWhenTotalMeetsThreshold() {
        when(watermarkPort.get("recharge_achievement_judge")).thenReturn(Optional.empty());
        when(rechargePort.usersWithNewRechargeSince(any(), any())).thenReturn(List.of(42L));
        when(catalogPort.activeDefinitions()).thenReturn(List.of(
                def("recharge_amount_1", "recharge_total_amount", new BigDecimal("0.01"))));
        when(unlockPort.unlockedCodes(42)).thenReturn(Set.of());
        when(rechargePort.totalRecharged(42)).thenReturn(new BigDecimal("30"));

        job(true).judgeDaily();

        verify(unlockPort).unlock(eq(42L), eq("recharge_amount_1"), any(), eq(5), eq("batch_scan"));
    }

    @Test
    void unlocksConsistencyAchievementUsingDedicatedCheckNotTotal() {
        when(watermarkPort.get("recharge_achievement_judge")).thenReturn(Optional.empty());
        when(rechargePort.usersWithNewRechargeSince(any(), any())).thenReturn(List.of(7L));
        when(catalogPort.activeDefinitions()).thenReturn(List.of(
                def("recharge_consistency_1", "recharge_consecutive_months", new BigDecimal("3"))));
        when(unlockPort.unlockedCodes(7)).thenReturn(Set.of());
        when(rechargePort.hasThreeConsecutiveMonthsOfRecharge(7)).thenReturn(true);

        job(true).judgeDaily();

        verify(unlockPort).unlock(eq(7L), eq("recharge_consistency_1"), any(), eq(5), eq("batch_scan"));
    }
}
