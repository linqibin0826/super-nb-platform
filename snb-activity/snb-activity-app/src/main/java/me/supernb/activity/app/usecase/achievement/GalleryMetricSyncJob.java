package me.supernb.activity.app.usecase.achievement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.GalleryAchievementSignalPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 造像车间成就 metric 生产者(日频)。
@Slf4j
@Service
public class GalleryMetricSyncJob {

    private final GalleryAchievementSignalPort signalPort;
    private final UserMetricPort metricPort;
    private final CheckinSettlementProperties settlementProperties;

    public GalleryMetricSyncJob(GalleryAchievementSignalPort signalPort, UserMetricPort metricPort,
            CheckinSettlementProperties settlementProperties) {
        this.signalPort = signalPort;
        this.metricPort = metricPort;
        this.settlementProperties = settlementProperties;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Shanghai")
    public void syncDaily() {
        if (!settlementProperties.scanEnabled()) {
            log.info("造像车间 metric 同步已跳过:scanEnabled=false");
            return;
        }
        writeSafely("gallery_generate_done_count", signalPort::generationDoneCounts);
        writeSafely("gallery_like_fav_count", signalPort::likeAndFavoriteCounts);
    }

    private void writeSafely(String metricCode,
            java.util.function.Supplier<List<GalleryAchievementSignalPort.UserCount>> supplier) {
        try {
            Map<Long, Double> values = supplier.get().stream()
                    .collect(Collectors.toMap(GalleryAchievementSignalPort.UserCount::userId,
                            c -> (double) c.count()));
            metricPort.upsertBatch(metricCode, values);
        } catch (Exception e) {
            log.error("造像车间 metric 同步失败 metricCode={}", metricCode, e);
        }
    }
}
