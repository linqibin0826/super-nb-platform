package me.supernb.activity.app.usecase.usageboard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongPredicate;
import me.supernb.activity.domain.model.read.usage.BoardEntry;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.BoardView;
import me.supernb.activity.domain.model.read.usage.BoardView.Gap;
import me.supernb.activity.domain.model.read.usage.BoardView.MeView;
import me.supernb.activity.domain.model.read.usage.BoardView.TierGap;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;

/// 用量榜组装器(静态纯函数,无框架依赖):排序定名次、算百分位/档位/环比,
/// 并按请求装配对外 [BoardView](Top50 + 我的位置 + 邻域),出处 spec §5/§7。
public final class BoardAssembler {

    /// Top 榜单最多展示的行数(出处 spec §5)。
    private static final int TOP_SIZE = 50;

    /// 邻域半径:以我为中心前后各取 5 名,含本人共 11 行(出处 spec §5)。
    private static final int NEIGHBORHOOD_RADIUS = 5;

    /// 工具类,禁止实例化。
    private BoardAssembler() {
    }

    /// 刷新时调用一次:把一批用量行按 tokens/cost 两个指标各排一遍、定连续名次、
    /// 算百分位/档位,并结合上一次快照的名次映射算出环比,组成完整数据集
    /// (排序/名次/百分位口径出处 spec §5)。
    ///
    /// @param rows           窗口内每用户的用量聚合行
    /// @param prevTokensRank 上一次快照的 tokens 榜名次(userId → rank),无历史时传空 map
    /// @param prevCostRank   上一次快照的 cost 榜名次(userId → rank),无历史时传空 map
    /// @param updatedAt      本次数据更新时刻
    /// @param periodEndsAt   本周期结束时刻,可为空
    /// @return 双指标已排序、名次/百分位/档位/环比就绪的数据集
    public static BoardDataset assemble(List<UsageBoardRow> rows, Map<Long, Integer> prevTokensRank,
            Map<Long, Integer> prevCostRank, Instant updatedAt, Instant periodEndsAt) {
        int participants = rows.size();
        List<RankedRow> byTokens = rankRows(rows, participants, tokensOrder(), prevTokensRank);
        List<RankedRow> byCost = rankRows(rows, participants, costOrder(), prevCostRank);
        return new BoardDataset(updatedAt, periodEndsAt, participants, byTokens, byCost);
    }

    /// 每次请求调用:从数据集取出对应指标榜,裁出 Top50、装配当前用户位置
    /// (不在榜时才懒查 eligible)与邻域片段,拼成对外整页视图
    /// (me/邻域/档位差距口径出处 spec §5/§7)。
    ///
    /// @param ds       {@link #assemble} 产出的数据集
    /// @param period   榜单周期口径(原样回显)
    /// @param metric   榜单排序口径,决定取 byTokens 还是 byCost
    /// @param userId   当前请求用户 id
    /// @param eligible 用户是否满足上榜门槛的判定,仅当用户不在数据集里才调用
    /// @return 对外用量榜整页视图
    public static BoardView view(BoardDataset ds, BoardPeriod period, BoardMetric metric,
            long userId, LongPredicate eligible) {
        List<RankedRow> ranked = metric == BoardMetric.TOKENS ? ds.byTokens() : ds.byCost();
        List<BoardEntry> top = toEntries(ranked.subList(0, Math.min(TOP_SIZE, ranked.size())));
        String periodStr = period.name().toLowerCase();
        String metricStr = metric.name().toLowerCase();

        RankedRow mine = findMine(ranked, userId);
        if (mine == null) {
            String meStatus = eligible.test(userId) ? "no_usage" : "not_eligible";
            return new BoardView(periodStr, metricStr, ds.updatedAt(), ds.periodEndsAt(),
                    ds.participants(), top, null, meStatus, List.of());
        }

        MeView me = buildMeView(mine, ranked, metric);
        List<BoardEntry> neighborhood = mine.rank() > TOP_SIZE
                ? toEntries(neighborhoodSlice(ranked, mine.rank()))
                : List.of();
        return new BoardView(periodStr, metricStr, ds.updatedAt(), ds.periodEndsAt(),
                ds.participants(), top, me, null, neighborhood);
    }

    /// 快照任务复用:算出 tokens 榜的裸名次映射(userId → rank),不含百分位/档位/环比
    /// (排序口径出处 spec §5)。
    ///
    /// @param rows 窗口内每用户的用量聚合行
    /// @return userId 到 tokens 榜名次的映射
    public static Map<Long, Integer> rankByTokens(List<UsageBoardRow> rows) {
        return rankMap(rows, tokensOrder());
    }

    /// 快照任务复用:算出 cost 榜的裸名次映射(userId → rank),不含百分位/档位/环比
    /// (排序口径出处 spec §5)。
    ///
    /// @param rows 窗口内每用户的用量聚合行
    /// @return userId 到 cost 榜名次的映射
    public static Map<Long, Integer> rankByCost(List<UsageBoardRow> rows) {
        return rankMap(rows, costOrder());
    }

    // tokens 降序,并列按 userId 升序 tie-break(出处 spec §5)。
    private static Comparator<UsageBoardRow> tokensOrder() {
        return Comparator.comparingLong(UsageBoardRow::tokens).reversed()
                .thenComparingLong(UsageBoardRow::userId);
    }

    // cost 降序,并列按 userId 升序 tie-break(出处 spec §5)。
    private static Comparator<UsageBoardRow> costOrder() {
        return Comparator.comparingDouble(UsageBoardRow::cost).reversed()
                .thenComparingLong(UsageBoardRow::userId);
    }

    // 排序后逐行定连续名次(1,2,3…,并列也不同名)、算百分位/档位/环比。
    private static List<RankedRow> rankRows(List<UsageBoardRow> rows, int participants,
            Comparator<UsageBoardRow> order, Map<Long, Integer> prevRank) {
        List<UsageBoardRow> sorted = new ArrayList<>(rows);
        sorted.sort(order);
        List<RankedRow> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            UsageBoardRow r = sorted.get(i);
            int rank = i + 1;
            int percentile = (participants - rank) * 100 / participants;
            Integer prev = prevRank.get(r.userId());
            Integer delta = prev == null ? null : prev - rank;
            result.add(new RankedRow(rank, r.userId(), r.displayName(), r.avatarUrl(), r.tokens(),
                    r.requests(), r.cost(), CostTiers.tier(r.cost()), percentile, delta));
        }
        return result;
    }

    // rankByTokens/rankByCost 共用:只排序定名次,不算百分位/档位/环比。
    private static Map<Long, Integer> rankMap(List<UsageBoardRow> rows, Comparator<UsageBoardRow> order) {
        List<UsageBoardRow> sorted = new ArrayList<>(rows);
        sorted.sort(order);
        Map<Long, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            map.put(sorted.get(i).userId(), i + 1);
        }
        return map;
    }

    private static RankedRow findMine(List<RankedRow> ranked, long userId) {
        for (RankedRow r : ranked) {
            if (r.userId() == userId) {
                return r;
            }
        }
        return null;
    }

    // TOKENS:gapToNext/behind 取自相邻名次的行,榜首/末位对应一侧为 null;
    // AMOUNT:gapToNext/behind 恒为 null,改用 gapToNextTier(出处 spec §5/§7)。
    private static MeView buildMeView(RankedRow mine, List<RankedRow> ranked, BoardMetric metric) {
        Gap gapToNext = null;
        Gap behind = null;
        TierGap gapToNextTier = null;
        if (metric == BoardMetric.TOKENS) {
            if (mine.rank() > 1) {
                RankedRow above = ranked.get(mine.rank() - 2);
                gapToNext = new Gap(above.displayName(), above.tokens() - mine.tokens());
            }
            if (mine.rank() < ranked.size()) {
                RankedRow below = ranked.get(mine.rank());
                behind = new Gap(below.displayName(), mine.tokens() - below.tokens());
            }
        } else {
            gapToNextTier = CostTiers.nextTierGap(mine.cost());
        }
        return new MeView(mine.rank(), mine.tokens(), mine.requests(), mine.cost(), mine.costTier(),
                mine.percentile(), mine.delta(), gapToNext, behind, gapToNextTier);
    }

    // 以 rank 为中心 [rank-5, rank+5],与榜界([1, size])裁剪(出处 spec §5)。
    private static List<RankedRow> neighborhoodSlice(List<RankedRow> ranked, int rank) {
        int lo = Math.max(1, rank - NEIGHBORHOOD_RADIUS);
        int hi = Math.min(ranked.size(), rank + NEIGHBORHOOD_RADIUS);
        return ranked.subList(lo - 1, hi);
    }

    // 剥掉精确 cost,只留脱敏后的 costTier——对外行永不带精确金额(红线,出处 spec §7)。
    private static List<BoardEntry> toEntries(List<RankedRow> rows) {
        List<BoardEntry> result = new ArrayList<>(rows.size());
        for (RankedRow r : rows) {
            result.add(new BoardEntry(r.rank(), r.displayName(), r.avatarUrl(), r.tokens(),
                    r.requests(), r.costTier(), r.percentile(), r.delta()));
        }
        return result;
    }
}
