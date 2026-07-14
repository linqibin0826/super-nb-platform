package me.supernb.activity.app.usecase.achievement;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.read.AccountAnniversaryReadPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 工龄成就直判(日频,不入 user_metric——"account_age_days 判定时现算,纯函数不入表",
/// 深化稿 §6.1)。逐条阈值(100/365 天)反查"今天恰好满 N 天"的候选,零全表扫描。
@Slf4j
@Service
public class AccountAnniversaryJudgeJob {

    private final AccountAnniversaryReadPort anniversaryPort;
    private final AchievementCatalogPort catalogPort;
    private final AchievementUnlockPort unlockPort;
    private final CheckinSettlementProperties settlementProperties;

    public AccountAnniversaryJudgeJob(AccountAnniversaryReadPort anniversaryPort, AchievementCatalogPort catalogPort,
            AchievementUnlockPort unlockPort, CheckinSettlementProperties settlementProperties) {
        this.anniversaryPort = anniversaryPort;
        this.catalogPort = catalogPort;
        this.unlockPort = unlockPort;
        this.settlementProperties = settlementProperties;
    }

    @Scheduled(cron = "0 25 1 * * *", zone = "Asia/Shanghai")
    public void judgeDaily() {
        if (!settlementProperties.scanEnabled()) {
            log.info("工龄成就判定已跳过:scanEnabled=false");
            return;
        }
        List<AchievementDefinition> anniDefs = catalogPort.activeDefinitions().stream()
                .filter(d -> "account_age_days".equals(d.metricCode()))
                .toList();
        for (AchievementDefinition def : anniDefs) {
            int days = def.thresholdValue().intValue();
            for (long userId : anniversaryPort.registeredExactlyDaysAgo(days)) {
                try {
                    if (!unlockPort.unlockedCodes(userId).contains(def.code())) {
                        unlockPort.unlock(userId, def.code(), Instant.now(), def.nbPoints(), "batch_scan");
                    }
                } catch (Exception e) {
                    log.error("工龄成就判定失败 user={} code={}", userId, def.code(), e);
                }
            }
        }
    }
}
