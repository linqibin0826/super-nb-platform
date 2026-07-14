package me.supernb.activity.app.usecase.achievement;

import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.LeaderboardAchievementSignalPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 排行榜成就 metric 生产者(日频,00:10——晚于既有 RankSnapshotJob 的 00:05,
/// 早于 30 天清理窗口,深化稿 §6.3)。
@Slf4j
@Service
public class LeaderboardMetricSyncJob {

    private final LeaderboardAchievementSignalPort signalPort;
    private final UserMetricPort metricPort;
    private final CheckinSettlementProperties settlementProperties;

    public LeaderboardMetricSyncJob(LeaderboardAchievementSignalPort signalPort, UserMetricPort metricPort,
            CheckinSettlementProperties settlementProperties) {
        this.signalPort = signalPort;
        this.metricPort = metricPort;
        this.settlementProperties = settlementProperties;
    }

    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Shanghai")
    public void syncDaily() {
        if (!settlementProperties.scanEnabled()) {
            log.info("排行榜 metric 同步已跳过:scanEnabled=false");
            return;
        }
        Map<Long, Double> values = signalPort.bestRankEver().stream()
                .collect(Collectors.toMap(LeaderboardAchievementSignalPort.UserRank::userId,
                        r -> (double) r.bestRankEver()));
        metricPort.upsertBatch("leaderboard_best_rank_ever", values);
    }
}
