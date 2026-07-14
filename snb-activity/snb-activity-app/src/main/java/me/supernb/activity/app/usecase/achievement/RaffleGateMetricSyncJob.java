package me.supernb.activity.app.usecase.achievement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.RaffleGateAchievementSignalPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 联动矩阵成就 metric 生产者(日频)。五个信号各自独立 try/catch,任一异常不阻断其余
/// (仿 RankSnapshotJob"逐组失败只记日志"惯例)。
@Slf4j
@Service
public class RaffleGateMetricSyncJob {

    private final RaffleGateAchievementSignalPort signalPort;
    private final UserMetricPort metricPort;
    private final CheckinSettlementProperties settlementProperties;

    public RaffleGateMetricSyncJob(RaffleGateAchievementSignalPort signalPort, UserMetricPort metricPort,
            CheckinSettlementProperties settlementProperties) {
        this.signalPort = signalPort;
        this.metricPort = metricPort;
        this.settlementProperties = settlementProperties;
    }

    @Scheduled(cron = "0 5 1 * * *", zone = "Asia/Shanghai")
    public void syncDaily() {
        if (!settlementProperties.scanEnabled()) {
            log.info("联动矩阵 metric 同步已跳过:scanEnabled=false");
            return;
        }
        writeSafely("raffle_entry_count", signalPort::raffleEntryCounts);
        writeSafely("raffle_win_count", signalPort::raffleWinCounts);
        writeSafely("raffle_companion_count", signalPort::raffleCompanionCounts);
        writeSafely("gate_win_count", signalPort::gateWinCounts);
        writeSafely("drawcard_count", signalPort::drawcardCounts);
    }

    private void writeSafely(String metricCode,
            java.util.function.Supplier<List<RaffleGateAchievementSignalPort.UserCount>> supplier) {
        try {
            Map<Long, Double> values = supplier.get().stream()
                    .collect(Collectors.toMap(RaffleGateAchievementSignalPort.UserCount::userId,
                            c -> (double) c.count()));
            metricPort.upsertBatch(metricCode, values);
        } catch (Exception e) {
            log.error("联动矩阵 metric 同步失败 metricCode={}", metricCode, e);
        }
    }
}
