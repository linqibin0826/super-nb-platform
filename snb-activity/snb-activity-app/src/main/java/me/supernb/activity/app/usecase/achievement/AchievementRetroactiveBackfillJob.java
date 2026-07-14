package me.supernb.activity.app.usecase.achievement;

import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.achievement.config.AchievementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 成就首刷批处理:一次性把 AchievementJudgeEngine 的判定逻辑跑遍全部曾经有 user_metric
/// 记录的存量用户,unlock_source=retroactive_backfill。是否已运行过靠水位线是否存在门控——
/// 门控本身就保证"至多跑一次",`@Scheduled(fixedDelay=Long.MAX_VALUE)` 只是"应用启动后
/// 尝试跑一次"的常见惯用法,真正的幂等保证在水位线检查。只补零成本层徽章,不触发补给资格
/// (Plan A 判定窗口已是"当月新增",与本任务无交集,深化稿 §4)。
@Slf4j
@Service
public class AchievementRetroactiveBackfillJob {

    private static final String JOB_NAME = "achievement_retroactive_backfill";

    private final UserMetricPort metricPort;
    private final AchievementCatalogPort catalogPort;
    private final ScanWatermarkPort watermarkPort;
    private final AchievementJudgeEngine judgeEngine;
    private final AchievementProperties achievementProperties;

    /// 构造:注入指标底座、目录、水位线、判定引擎(复用其包内可见判定方法)与本计划专属配置。
    public AchievementRetroactiveBackfillJob(UserMetricPort metricPort, AchievementCatalogPort catalogPort,
            ScanWatermarkPort watermarkPort, AchievementJudgeEngine judgeEngine,
            AchievementProperties achievementProperties) {
        this.metricPort = metricPort;
        this.catalogPort = catalogPort;
        this.watermarkPort = watermarkPort;
        this.judgeEngine = judgeEngine;
        this.achievementProperties = achievementProperties;
    }

    /// 应用启动 3 分钟后尝试跑一次(fixedDelay 设为 Long.MAX_VALUE 保证不会自动重复触发;
    /// 真正的"至多一次"由水位线是否存在决定,即便应用重启也不会重跑)。
    @Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 180_000)
    public void runOnce() {
        if (!achievementProperties.retroactiveBackfillEnabled()) {
            log.info("成就首刷未开启(CHECKIN_RETROACTIVE_BACKFILL_ENABLED=false),跳过");
            return;
        }
        if (watermarkPort.get(JOB_NAME).isPresent()) {
            log.info("成就首刷已运行过(水位线存在),跳过");
            return;
        }
        List<Long> allUsers = metricPort.usersUpdatedSince(Instant.EPOCH);
        List<AchievementDefinition> allDefs = catalogPort.activeDefinitions();
        List<AchievementDefinition> metricDefs = AchievementJudgeEngine.metricThresholdDefs(allDefs);
        List<AchievementDefinition> metaLikeDefs = AchievementJudgeEngine.metaLikeDefs(allDefs);

        for (long userId : allUsers) {
            try {
                judgeEngine.judgeMetricThresholds(userId, metricDefs, "retroactive_backfill");
            } catch (Exception e) {
                log.error("成就首刷判定失败(metric_threshold) user={}", userId, e);
            }
        }
        for (long userId : allUsers) {
            try {
                judgeEngine.judgeMetaLike(userId, metaLikeDefs, allDefs, "retroactive_backfill");
            } catch (Exception e) {
                log.error("成就首刷判定失败(meta_combo) user={}", userId, e);
            }
        }
        watermarkPort.advance(JOB_NAME, Instant.now());
        log.info("成就首刷批处理完成,共处理 {} 名存量用户", allUsers.size());
    }
}
