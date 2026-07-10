package me.supernb.activity.app.usecase.usageboard;

import java.time.Instant;
import java.util.List;

/// 一个周期的完整数据集:双指标(tokens/cost)各自排好序、算好名次/百分位/档位/环比,
/// 由缓存持有,每次刷新调用 [BoardAssembler#assemble] 生成一份,请求时只读切片装配视图。
///
/// @param updatedAt    本次数据更新时刻
/// @param periodEndsAt 本周期结束时刻,可为空(如 ALL 全程无固定结束时刻)
/// @param participants 参与人数(满足上榜门槛的用户数)
/// @param byTokens     按 tokens 降序排好名次的行(并列按 userId 升序,出处 spec §5)
/// @param byCost       按 cost 降序排好名次的行(并列按 userId 升序,出处 spec §5)
public record BoardDataset(Instant updatedAt, Instant periodEndsAt, int participants,
                           List<RankedRow> byTokens, List<RankedRow> byCost) {
}

/// 组装器内部行:携带精确 cost,仅供 app 层内部计算差距/档位使用,红线——绝不越过
/// [BoardAssembler] 出现在对外 [me.supernb.activity.domain.model.read.usage.BoardEntry]
/// (出处 spec §7)。
///
/// @param rank        名次(从 1 开始连续;指标值并列也占不同名次,出处 spec §5)
/// @param userId      用户 id
/// @param displayName 脱敏后的展示名
/// @param avatarUrl   头像地址,可为空
/// @param tokens      窗口内 token 用量之和
/// @param requests    窗口内请求次数之和
/// @param cost        窗口内实际花费之和(精确值,红线:不得流入 BoardEntry,出处 spec §7)
/// @param costTier    花费档位
/// @param percentile  百分位(越大越靠前)
/// @param delta       相对上一次快照的名次变化,无历史快照时为 null
record RankedRow(int rank, long userId, String displayName, String avatarUrl, long tokens,
                 long requests, double cost, String costTier, int percentile, Integer delta) {
}
