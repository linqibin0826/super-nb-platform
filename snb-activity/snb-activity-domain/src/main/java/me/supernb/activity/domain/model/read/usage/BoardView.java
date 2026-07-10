package me.supernb.activity.domain.model.read.usage;

import java.time.Instant;
import java.util.List;

/// 用量榜整页只读视图:Top 榜单 + 当前用户位置(me)+ 邻域片段,一次性拼装给前端。
///
/// @param period       榜单周期口径(回显请求参数)
/// @param metric       榜单排序口径(回显请求参数)
/// @param updatedAt    本次数据更新时刻
/// @param periodEndsAt 本周期结束时刻
/// @param participants 参与人数(满足上榜门槛的用户数)
/// @param top           Top 榜单条目列表
/// @param me            当前用户位置视图,未上榜/不满足门槛时为 null
/// @param meStatus      当前用户状态标识(未登录/无用量/不满足门槛等,语义见 Task 6)
/// @param neighborhood  当前用户名次前后邻域条目
public record BoardView(String period, String metric, Instant updatedAt,
                        Instant periodEndsAt, int participants,
                        List<BoardEntry> top, MeView me, String meStatus,
                        List<BoardEntry> neighborhood) {

    /// 当前用户在榜单中的位置视图(含与上下名次/档位的差距)。
    ///
    /// @param rank          当前用户名次
    /// @param tokens        当前用户窗口内 token 用量之和
    /// @param requests      当前用户窗口内请求次数之和
    /// @param cost          当前用户窗口内实际花费之和(仅本人可见精确值)
    /// @param costTier      当前用户花费档位
    /// @param percentile    当前用户百分位
    /// @param delta         相对上一次快照的名次变化,无历史快照时为 null
    /// @param gapToNext     与上一名的差距,已是第一名时为 null
    /// @param behind        与下一名的差距,已是最后一名时为 null
    /// @param gapToNextTier 与下一花费档位的差距,已是最高档位时为 null
    public record MeView(int rank, long tokens, long requests, double cost, String costTier,
                         int percentile, Integer delta, Gap gapToNext, Gap behind, TierGap gapToNextTier) {
    }

    /// 与相邻名次用户的差距(展示名 + token 差值)。
    ///
    /// @param displayName 相邻用户的脱敏展示名
    /// @param tokens      token 用量差值
    public record Gap(String displayName, long tokens) {
    }

    /// 与下一花费档位的差距。
    ///
    /// @param tier   目标档位名称
    /// @param amount 花费差值(元)
    public record TierGap(String tier, double amount) {
    }
}
