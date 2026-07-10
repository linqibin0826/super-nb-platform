package me.supernb.activity.domain.model.read.usage;

/// 榜单条目(公开展示;红线:不含 cost 字段,只暴露 costTier/percentile 等脱敏后的相对位置)。
///
/// @param rank        名次(从 1 开始)
/// @param displayName 脱敏后的展示名
/// @param avatarUrl   头像地址,可为空
/// @param tokens      窗口内 token 用量之和
/// @param requests    窗口内请求次数之和
/// @param costTier    花费档位(脱敏后的分档展示,不出精确金额)
/// @param percentile  百分位(越大越靠前)
/// @param delta       相对上一次快照的名次变化,无历史快照时为 null
public record BoardEntry(int rank, String displayName, String avatarUrl, long tokens,
                         long requests, String costTier, int percentile, Integer delta) {
}
