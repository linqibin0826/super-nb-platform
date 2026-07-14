package me.supernb.activity.app.usecase.achievement;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.UsageMetricSignalPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 用量相关成就 metric 生产者。小时级增量(唯一禁止现查全表的轴):计数累加器用
/// 无重叠 + 30 秒提交安全边际的严格递进窗口(防重复计数);深夜旗标是幂等指标,
/// 额外把窗口下界往回多看,兜底漏跑(两个指标窗口策略故意不同,见任务说明)。
/// 日终峰值(23:30)现查当天,不增量,只在超过既有峰值时更新。
@Slf4j
@Service
public class UsageMetricSyncJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final String JOB_NAME = "usage_metric_sync";
    private static final Duration COMMIT_SAFETY_MARGIN = Duration.ofSeconds(30);
    private static final Duration LATE_NIGHT_LOOKBACK_OVERLAP = Duration.ofMinutes(15);
    private static final Duration FIRST_RUN_LOOKBACK = Duration.ofDays(1);

    private final UsageMetricSignalPort signalPort;
    private final UserMetricPort metricPort;
    private final ScanWatermarkPort watermarkPort;
    private final CheckinSettlementProperties settlementProperties;

    /// 构造:注入用量信号端口、指标底座、水位线端口与 Plan A 的批处理总闸配置。
    public UsageMetricSyncJob(UsageMetricSignalPort signalPort, UserMetricPort metricPort,
            ScanWatermarkPort watermarkPort, CheckinSettlementProperties settlementProperties) {
        this.signalPort = signalPort;
        this.metricPort = metricPort;
        this.watermarkPort = watermarkPort;
        this.settlementProperties = settlementProperties;
    }

    /// 每小时入口(启动 1 分钟后开始,之后每小时一次)。
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void syncHourly() {
        if (!settlementProperties.scanEnabled()) {
            log.info("用量 metric 小时同步已跳过:scanEnabled=false");
            return;
        }
        Instant safeCutoff = Instant.now().minus(COMMIT_SAFETY_MARGIN);
        Instant since = watermarkPort.get(JOB_NAME).orElse(safeCutoff.minus(FIRST_RUN_LOOKBACK));
        if (!safeCutoff.isAfter(since)) {
            return; // 距上次运行不足安全边际,本轮空转,不推进水位线
        }
        for (var entry : signalPort.callCountsSince(since, safeCutoff).entrySet()) {
            double current = metricPort.value(entry.getKey(), "api_call_total_count").orElse(0.0);
            metricPort.upsert(entry.getKey(), "api_call_total_count", current + entry.getValue());
        }
        // 深夜旗标幂等,可安全扩大回看窗口兜底漏跑(不影响计数器的严格递进窗口)
        Instant lateNightScanStart = since.minus(LATE_NIGHT_LOOKBACK_OVERLAP);
        for (var entry : signalPort.lateNightFlagsSince(lateNightScanStart, safeCutoff).entrySet()) {
            if (entry.getValue()) {
                metricPort.upsert(entry.getKey(), "api_call_late_night_flag", 1);
            }
        }
        watermarkPort.advance(JOB_NAME, safeCutoff);
    }

    /// 每日 23:30(Asia/Shanghai)日终峰值结算。
    @Scheduled(cron = "0 30 23 * * *", zone = "Asia/Shanghai")
    public void syncDailyPeak() {
        if (!settlementProperties.scanEnabled()) {
            log.info("用量 metric 日终峰值同步已跳过:scanEnabled=false");
            return;
        }
        for (var entry : signalPort.callCountsOnDay(LocalDate.now(ZONE), ZONE).entrySet()) {
            double currentPeak = metricPort.value(entry.getKey(), "api_call_daily_peak_max").orElse(0.0);
            if (entry.getValue() > currentPeak) {
                metricPort.upsert(entry.getKey(), "api_call_daily_peak_max", entry.getValue());
            }
        }
    }
}
