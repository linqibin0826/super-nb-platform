package me.supernb.activity.app.usecase.achievement.query;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.supernb.activity.domain.model.achievement.AchievementCategoryView;
import me.supernb.activity.domain.model.achievement.AchievementDefinition;
import me.supernb.activity.domain.model.achievement.AchievementItemView;
import me.supernb.activity.domain.model.achievement.AchievementSeriesView;
import me.supernb.activity.domain.model.achievement.AchievementStepView;
import me.supernb.activity.domain.model.achievement.AchievementSummaryView;
import me.supernb.activity.domain.model.achievement.AchievementUnlock;
import me.supernb.activity.domain.model.achievement.AchievementWallView;
import me.supernb.activity.domain.model.achievement.ProgressView;
import me.supernb.activity.domain.port.achievement.AchievementCatalogPort;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.domain.port.nb.NbLedgerPort;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import org.springframework.stereotype.Service;

/// 成就墙查询(spec/深化稿 §6;字段形状按前端接线计划契约总览钉死)。"元编年史"类目单独
/// 抽出成 metaAchievements[],不进 categories[];机密档案类目(全体 hiddenReveal=true)恒不建
/// series 分组,即便个别行带 series_code/tier_level(与 Task 1 seed 注释同一条纪律)。
@Service
public class AchievementWallQueryService {

    private static final String META_CATEGORY = "元编年史";
    private static final Set<String> EXCLUSIVE_CODES =
            Set.of("exclusive_founding_issue", "exclusive_founding_fullmonth");
    private static final NumberFormat INT_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    private final AchievementCatalogPort catalogPort;
    private final AchievementUnlockPort unlockPort;
    private final UserMetricPort metricPort;
    private final NbLedgerPort nbLedger;

    /// 构造:注入目录只读端口、解锁台账端口、指标底座端口与 NB 账本读端口。
    public AchievementWallQueryService(AchievementCatalogPort catalogPort, AchievementUnlockPort unlockPort,
            UserMetricPort metricPort, NbLedgerPort nbLedger) {
        this.catalogPort = catalogPort;
        this.unlockPort = unlockPort;
        this.metricPort = metricPort;
        this.nbLedger = nbLedger;
    }

    /// 组装某用户的成就墙整页视图。
    public AchievementWallView wall(long userId) {
        List<AchievementDefinition> allDefs = catalogPort.allDefinitions();
        Map<String, String> seriesLabels = catalogPort.allSeriesLabels();
        Map<String, Double> metrics = metricPort.allMetrics(userId);
        List<AchievementUnlock> unlocks = unlockPort.myUnlocks(userId);
        Map<String, AchievementUnlock> unlockByCode =
                unlocks.stream().collect(Collectors.toMap(AchievementUnlock::achievementCode, u -> u, (a, b) -> a));

        List<AchievementDefinition> metaDefs = allDefs.stream()
                .filter(d -> META_CATEGORY.equals(d.category()))
                .sorted(Comparator.comparingInt(AchievementDefinition::sortOrder))
                .toList();
        List<AchievementDefinition> regularDefs =
                allDefs.stream().filter(d -> !META_CATEGORY.equals(d.category())).toList();

        List<AchievementCategoryView> categories = buildCategories(regularDefs, seriesLabels, metrics, unlockByCode);
        List<AchievementItemView> metaAchievements = metaDefs.stream()
                .map(d -> toItemView(d, unlockByCode.get(d.code()), 0))
                .toList();

        int totalCount = allDefs.size();
        int unlockedCount = unlocks.size();
        int nbTotal = nbLedger.totalPoints(userId); // 口径切换:账本 SUM 单真源(打卡+成就,替代成就点数内存和)
        List<String> pendingUnseal =
                unlocks.stream().filter(u -> !u.seen()).map(AchievementUnlock::achievementCode).toList();

        return new AchievementWallView(new AchievementSummaryView(unlockedCount, totalCount, nbTotal),
                categories, metaAchievements, pendingUnseal);
    }

    private static List<AchievementCategoryView> buildCategories(List<AchievementDefinition> defs,
            Map<String, String> seriesLabels, Map<String, Double> metrics,
            Map<String, AchievementUnlock> unlockByCode) {
        List<AchievementDefinition> sorted =
                defs.stream().sorted(Comparator.comparingInt(AchievementDefinition::sortOrder)).toList();
        Map<String, List<AchievementDefinition>> byCategory = new LinkedHashMap<>();
        for (AchievementDefinition d : sorted) {
            byCategory.computeIfAbsent(d.category(), k -> new ArrayList<>()).add(d);
        }

        List<AchievementCategoryView> result = new ArrayList<>();
        for (Map.Entry<String, List<AchievementDefinition>> entry : byCategory.entrySet()) {
            List<AchievementDefinition> catDefs = entry.getValue();
            boolean hidden = !catDefs.isEmpty() && catDefs.stream().allMatch(AchievementDefinition::hiddenReveal);

            List<AchievementItemView> items = new ArrayList<>();
            Map<String, List<AchievementDefinition>> bySeries = new LinkedHashMap<>();
            int hiddenOrdinal = 0;
            for (AchievementDefinition d : catDefs) {
                if (hidden) {
                    hiddenOrdinal++;
                }
                // 机密档案纪律:隐藏类目恒不建 series 分组,即便个别行带 series_code/tier_level。
                if (!hidden && d.seriesCode() != null) {
                    bySeries.computeIfAbsent(d.seriesCode(), k -> new ArrayList<>()).add(d);
                } else {
                    items.add(toItemView(d, unlockByCode.get(d.code()), hiddenOrdinal));
                }
            }

            List<AchievementSeriesView> series = new ArrayList<>();
            for (Map.Entry<String, List<AchievementDefinition>> se : bySeries.entrySet()) {
                List<AchievementDefinition> steps = se.getValue().stream()
                        .sorted(Comparator.comparingInt(d -> d.tierLevel() == null ? 0 : d.tierLevel()))
                        .toList();
                String seriesName = seriesLabels.getOrDefault(se.getKey(), se.getKey());
                List<AchievementStepView> stepViews =
                        steps.stream().map(d -> toStepView(d, unlockByCode.get(d.code()))).toList();
                series.add(new AchievementSeriesView(seriesName, seriesProgress(steps, metrics, unlockByCode),
                        stepViews));
            }

            result.add(new AchievementCategoryView(entry.getKey(), hidden, items, series));
        }
        return result;
    }

    /// 单条(items[]/metaAchievements[])视图映射。`hiddenOrdinal` 是该条在其(机密)类目内
    /// 按 sort_order 的第几条(1-based),用于拼 "曾是机密档案 #0X" 角标;非隐藏类目传 0 不使用。
    private static AchievementItemView toItemView(AchievementDefinition def, AchievementUnlock unlock,
            int hiddenOrdinal) {
        boolean unlocked = unlock != null;
        int tier = rarityToTier(def.rarity());
        if (def.hiddenReveal() && !unlocked) {
            return new AchievementItemView(def.code(), null, null, tier, def.nbPoints(), false, def.status(), true,
                    null, null, null, null, null, def.hiddenHintText());
        }
        String revealedLabel = def.hiddenReveal() ? "曾是机密档案 #" + String.format("%02d", hiddenOrdinal) : null;
        String exclusiveTag = EXCLUSIVE_CODES.contains(def.code()) ? "绝版" : null;
        return new AchievementItemView(def.code(), def.name(), def.conditionText(), tier, def.nbPoints(), unlocked,
                def.status(), def.hiddenReveal(), def.flavorText(), null, exclusiveTag, revealedLabel, null, null);
    }

    /// 系列步骤视图映射——MVP 42 条里没有隐藏系列,故不做 hiddenReveal 涂黑处理
    /// (若未来出现隐藏系列,需要回头把这里也接上密封逻辑,当前不做超前设计)。
    private static AchievementStepView toStepView(AchievementDefinition def, AchievementUnlock unlock) {
        return new AchievementStepView(def.code(), def.name(), def.conditionText(), rarityToTier(def.rarity()),
                def.nbPoints(), unlock != null, def.status(), def.hiddenReveal(), def.flavorText());
    }

    /// 系列进度条:对准"下一个未达标层级"的阈值;全部达标时对准最高层阈值、pct 恒 100。
    /// comparator=lte(仅排行榜系列)时方向相反——数值越小越好,进度文案/百分比按反向公式给出
    /// (⚠️ 此 lte 分支公式未经真实数据校准,仅保证方向正确,系列进度条视觉验收后可能需要调整
    /// 这一个私有方法,不影响其余字段形状——同 Plan A gaugePct 的处理方式)。
    private static ProgressView seriesProgress(
            List<AchievementDefinition> stepsAsc, Map<String, Double> metrics,
            Map<String, AchievementUnlock> unlockByCode) {
        AchievementDefinition sample = stepsAsc.get(0);
        Double rawValue = metrics.get(sample.metricCode());
        boolean allDone = stepsAsc.stream().allMatch(d -> unlockByCode.containsKey(d.code()));
        AchievementDefinition target = allDone ? stepsAsc.get(stepsAsc.size() - 1)
                : stepsAsc.stream().filter(d -> !unlockByCode.containsKey(d.code())).findFirst()
                        .orElse(stepsAsc.get(stepsAsc.size() - 1));
        double threshold = target.thresholdValue().doubleValue();

        if ("lte".equals(sample.comparator())) {
            if (rawValue == null) {
                return new ProgressView(
                        "未上榜 / 前" + INT_FORMAT.format((long) threshold) + "名", 0);
            }
            int pct = allDone ? 100 : clampPct(threshold / rawValue * 100);
            return new ProgressView(
                    "第 " + INT_FORMAT.format(rawValue.longValue()) + " 名 / 前"
                            + INT_FORMAT.format((long) threshold) + "名",
                    pct);
        }
        double current = rawValue == null ? 0 : rawValue;
        double shown = allDone ? Math.min(current, threshold) : current;
        int pct = allDone ? 100 : clampPct(shown / threshold * 100);
        return new ProgressView(
                INT_FORMAT.format((long) shown) + " / " + INT_FORMAT.format((long) threshold), pct);
    }

    private static int clampPct(double raw) {
        return (int) Math.max(0, Math.min(100, Math.round(raw)));
    }

    private static int rarityToTier(String rarity) {
        return Integer.parseInt(rarity.substring(1));
    }
}
