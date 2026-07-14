package me.supernb.activity.app.usecase.achievement;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.scan.ScanWatermarkPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 统一成就判定引擎(每小时):只扫"近期 user_metric 有更新"的用户集,按 predicate_kind 分支
/// 判定。metric_threshold 里 metric_code="achievement_unlock_total_count"(meta_regular 用的
/// 合成指标)被移到与 meta_combo 同一阶段处理——严格晚于本轮全部普通 metric_threshold 解锁
/// 完成,不依赖 activeDefinitions() 的行返回顺序。类目/系列引用一律现查 activeDefinitions()
/// (深化稿增补#7动态引用),不写死列表。
@Slf4j
@Service
public class AchievementJudgeEngine {

    private static final String JOB_NAME = "achievement_judge_engine";
    private static final String SYNTHETIC_UNLOCK_COUNT_METRIC = "achievement_unlock_total_count";
    /// 合成指标②:双轴组合旗标(深化稿 §3「全栈选手」判定=api_call 存在 ∧ gallery 生成 ≥1,
    /// "均现成"——不由任何生产者落表,判定时现查两个既有 metric 派生,与 meta_regular 同款
    /// 合成指标处理(V9 seed 的 metric_code 保持原样,不改内容表)。
    private static final String SYNTHETIC_CROSS_SURFACE_METRIC = "cross_surface_flag";

    private final UserMetricPort metricPort;
    private final ScanWatermarkPort watermarkPort;
    private final AchievementCatalogPort catalogPort;
    private final AchievementUnlockPort unlockPort;
    private final CheckinSettlementProperties settlementProperties;

    /// 构造:注入指标底座、水位线、目录、解锁台账与 Plan A 的批处理总闸配置。
    public AchievementJudgeEngine(UserMetricPort metricPort, ScanWatermarkPort watermarkPort,
            AchievementCatalogPort catalogPort, AchievementUnlockPort unlockPort,
            CheckinSettlementProperties settlementProperties) {
        this.metricPort = metricPort;
        this.watermarkPort = watermarkPort;
        this.catalogPort = catalogPort;
        this.unlockPort = unlockPort;
        this.settlementProperties = settlementProperties;
    }

    /// 每小时入口(启动 2 分钟后开始,之后每小时一次)。
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 120_000)
    public void judgeHourly() {
        if (!settlementProperties.scanEnabled()) {
            log.info("成就判定引擎已跳过:scanEnabled=false");
            return;
        }
        Instant now = Instant.now();
        Instant since = watermarkPort.get(JOB_NAME).orElse(now.minus(Duration.ofDays(1)));
        List<Long> candidates = metricPort.usersUpdatedSince(since);
        if (candidates.isEmpty()) {
            watermarkPort.advance(JOB_NAME, now);
            return;
        }

        List<AchievementDefinition> allDefs = catalogPort.activeDefinitions();
        List<AchievementDefinition> metricDefs = metricThresholdDefs(allDefs);
        List<AchievementDefinition> metaLikeDefs = metaLikeDefs(allDefs);

        for (long userId : candidates) {
            try {
                judgeMetricThresholds(userId, metricDefs, "batch_scan");
            } catch (Exception e) {
                log.error("成就判定失败(metric_threshold) user={}", userId, e);
            }
        }
        for (long userId : candidates) {
            try {
                judgeMetaLike(userId, metaLikeDefs, allDefs, "batch_scan");
            } catch (Exception e) {
                log.error("成就判定失败(meta_combo/合成指标) user={}", userId, e);
            }
        }
        watermarkPort.advance(JOB_NAME, now);
    }

    /// metric_threshold 分支的候选定义(排除合成指标,那个挪到 metaLikeDefs 统一处理)。
    /// 包内可见,供首刷批处理复用,避免复制过滤逻辑。
    static List<AchievementDefinition> metricThresholdDefs(List<AchievementDefinition> allDefs) {
        return allDefs.stream()
                .filter(d -> "metric_threshold".equals(d.predicateKind())
                        && !SYNTHETIC_UNLOCK_COUNT_METRIC.equals(d.metricCode())
                        && !SYNTHETIC_CROSS_SURFACE_METRIC.equals(d.metricCode()))
                .toList();
    }

    /// meta_combo + 合成指标(meta_regular)的候选定义。包内可见,供首刷批处理复用。
    static List<AchievementDefinition> metaLikeDefs(List<AchievementDefinition> allDefs) {
        return allDefs.stream()
                .filter(d -> "meta_combo".equals(d.predicateKind())
                        || SYNTHETIC_UNLOCK_COUNT_METRIC.equals(d.metricCode())
                        || SYNTHETIC_CROSS_SURFACE_METRIC.equals(d.metricCode()))
                .toList();
    }

    /// metric_threshold 判定;unlockSource 由调用方指定("batch_scan" 常规批扫,
    /// "retroactive_backfill" 首刷)。包内可见供首刷批处理复用,不复制判定逻辑。
    void judgeMetricThresholds(long userId, List<AchievementDefinition> metricDefs, String unlockSource) {
        Set<String> alreadyUnlocked = unlockPort.unlockedCodes(userId);
        Map<String, Double> metrics = metricPort.allMetrics(userId);
        for (AchievementDefinition def : metricDefs) {
            if (alreadyUnlocked.contains(def.code()) || def.metricCode() == null) {
                continue;
            }
            Double value = metrics.get(def.metricCode());
            if (value != null && meetsThreshold(value, def.thresholdValue(), def.comparator())) {
                unlockPort.unlock(userId, def.code(), Instant.now(), def.nbPoints(), unlockSource);
            }
        }
    }

    /// meta_combo/合成指标判定;unlockSource 语义同上。包内可见供首刷批处理复用。
    void judgeMetaLike(long userId, List<AchievementDefinition> metaLikeDefs,
            List<AchievementDefinition> allDefs, String unlockSource) {
        // 重新查询(不复用 judgeMetricThresholds 阶段的旧集合),反映本轮已发生的最新解锁。
        Set<String> alreadyUnlocked = unlockPort.unlockedCodes(userId);
        for (AchievementDefinition def : metaLikeDefs) {
            if (alreadyUnlocked.contains(def.code())) {
                continue;
            }
            boolean qualifies = switch (def.code()) {
                case "meta_regular" -> unlockPort.unlockedCount(userId) >= def.thresholdValue().intValue();
                case "cross_surface_user" ->
                        metricPort.value(userId, "api_call_total_count").orElse(0.0) >= 1
                                && metricPort.value(userId, "gallery_generate_done_count").orElse(0.0) >= 1;
                case "meta_category_onboarding" ->
                        categoryFullyUnlocked(def.prerequisite(), allDefs, alreadyUnlocked);
                case "meta_series_master" -> anySeriesFullyUnlocked(allDefs, alreadyUnlocked);
                // 未来新增 meta_combo/合成指标条目须显式补分支,不做隐式默认放行
                // (深化稿不可逆清单第4条:判定逻辑变更=旧code退役+新code上线,不能静默改义)
                default -> {
                    log.warn("未识别的 meta_combo/合成指标 code,判定跳过:{}", def.code());
                    yield false;
                }
            };
            if (qualifies) {
                unlockPort.unlock(userId, def.code(), Instant.now(), def.nbPoints(), unlockSource);
            }
        }
    }

    /// 动态类目引用(深化稿增补#7):现查该类目下全部 active 非 meta 条目,不写死列表——
    /// 加新条目自动纳入判定,不必改这段代码。
    private static boolean categoryFullyUnlocked(String category, List<AchievementDefinition> allDefs,
            Set<String> unlockedCodes) {
        List<AchievementDefinition> inCategory = allDefs.stream()
                .filter(d -> category.equals(d.category()) && !"meta_combo".equals(d.predicateKind()))
                .toList();
        return !inCategory.isEmpty() && inCategory.stream().allMatch(d -> unlockedCodes.contains(d.code()));
    }

    /// 动态系列引用(深化稿增补#7):任一 series_code 下全部已启用层级点亮即算首次集齐。
    private static boolean anySeriesFullyUnlocked(List<AchievementDefinition> allDefs, Set<String> unlockedCodes) {
        Map<String, List<AchievementDefinition>> bySeries = allDefs.stream()
                .filter(d -> d.seriesCode() != null)
                .collect(Collectors.groupingBy(AchievementDefinition::seriesCode));
        for (var entry : bySeries.entrySet()) {
            if (entry.getValue().stream().allMatch(d -> unlockedCodes.contains(d.code()))) {
                return true;
            }
        }
        return false;
    }

    /// public(而非 private):Task 16 的 AchievementWallQueryService 在另一个包
    /// (app.usecase.achievement.query)里复用同一套阈值判定算系列进度条,包内可见不够用。
    public static boolean meetsThreshold(double value, BigDecimal threshold, String comparator) {
        if (threshold == null) {
            return false;
        }
        double t = threshold.doubleValue();
        return "lte".equals(comparator) ? value <= t : value >= t; // 未显式指定时默认 gte
    }
}
