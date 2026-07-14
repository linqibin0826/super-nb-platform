package me.supernb.activity.app.usecase.achievement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.read.AchievementRechargeReadPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 补给记录成就直判(日频,不入 user_metric——"现查透传不落表",深化稿 §6.1)。
@Slf4j
@Service
public class RechargeAchievementJudgeJob {

    private static final String JOB_NAME = "recharge_achievement_judge";

    private final AchievementRechargeReadPort rechargePort;
    private final AchievementCatalogPort catalogPort;
    private final AchievementUnlockPort unlockPort;
    private final ScanWatermarkPort watermarkPort;
    private final CheckinSettlementProperties settlementProperties;

    public RechargeAchievementJudgeJob(AchievementRechargeReadPort rechargePort, AchievementCatalogPort catalogPort,
            AchievementUnlockPort unlockPort, ScanWatermarkPort watermarkPort,
            CheckinSettlementProperties settlementProperties) {
        this.rechargePort = rechargePort;
        this.catalogPort = catalogPort;
        this.unlockPort = unlockPort;
        this.watermarkPort = watermarkPort;
        this.settlementProperties = settlementProperties;
    }

    @Scheduled(cron = "0 20 1 * * *", zone = "Asia/Shanghai")
    public void judgeDaily() {
        if (!settlementProperties.scanEnabled()) {
            log.info("补给记录成就判定已跳过:scanEnabled=false");
            return;
        }
        Instant now = Instant.now();
        Instant since = watermarkPort.get(JOB_NAME).orElse(now.minus(Duration.ofDays(2)));
        List<Long> candidates = rechargePort.usersWithNewRechargeSince(since, now);
        List<AchievementDefinition> recDefs = catalogPort.activeDefinitions().stream()
                .filter(d -> "补给记录".equals(d.category()))
                .toList();
        for (long userId : candidates) {
            try {
                judgeUser(userId, recDefs);
            } catch (Exception e) {
                log.error("补给记录成就判定失败 user={}", userId, e);
            }
        }
        watermarkPort.advance(JOB_NAME, now);
    }

    private void judgeUser(long userId, List<AchievementDefinition> recDefs) {
        Set<String> unlocked = unlockPort.unlockedCodes(userId);
        BigDecimal total = rechargePort.totalRecharged(userId);
        Boolean consistency = null; // 懒计算:只有真用到 recharge_consistency_1 才查三连窗口
        for (AchievementDefinition def : recDefs) {
            if (unlocked.contains(def.code())) {
                continue;
            }
            boolean qualifies;
            if ("recharge_consecutive_months".equals(def.metricCode())) {
                if (consistency == null) {
                    consistency = rechargePort.hasThreeConsecutiveMonthsOfRecharge(userId);
                }
                qualifies = consistency;
            } else {
                qualifies = total.compareTo(def.thresholdValue()) >= 0;
            }
            if (qualifies) {
                unlockPort.unlock(userId, def.code(), Instant.now(), def.nbPoints(), "batch_scan");
            }
        }
    }
}
