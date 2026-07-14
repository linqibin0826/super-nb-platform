package me.supernb.activity.app.usecase.achievement;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.ReferralAchievementSignalPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 拉新达人成就 metric 生产者(日频)。有效口径 = 被邀者 api_call_total_count ≥1
/// (决策④,收紧"纯注册计数"这个滥用史同款漏洞)。
@Slf4j
@Service
public class ReferralMetricSyncJob {

    private final ReferralAchievementSignalPort signalPort;
    private final UserMetricPort metricPort;
    private final CheckinSettlementProperties settlementProperties;

    /// 构造:注入拉新信号端口、指标底座与 Plan A 的批处理总闸配置。
    public ReferralMetricSyncJob(ReferralAchievementSignalPort signalPort, UserMetricPort metricPort,
            CheckinSettlementProperties settlementProperties) {
        this.signalPort = signalPort;
        this.metricPort = metricPort;
        this.settlementProperties = settlementProperties;
    }

    @Scheduled(cron = "0 15 1 * * *", zone = "Asia/Shanghai")
    public void syncDaily() {
        if (!settlementProperties.scanEnabled()) {
            log.info("拉新达人 metric 同步已跳过:scanEnabled=false");
            return;
        }
        Map<Long, Double> validCounts = new HashMap<>();
        for (var entry : signalPort.allInviteeIdsByInviter().entrySet()) {
            long validCount = entry.getValue().stream()
                    .filter(inviteeId -> metricPort.value(inviteeId, "api_call_total_count")
                            .map(v -> v >= 1).orElse(false))
                    .count();
            if (validCount > 0) {
                validCounts.put(entry.getKey(), (double) validCount);
            }
        }
        metricPort.upsertBatch("referral_valid_count", validCounts);
    }
}
