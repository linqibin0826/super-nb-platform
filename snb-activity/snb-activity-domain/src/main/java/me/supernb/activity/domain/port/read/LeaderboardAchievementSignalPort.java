package me.supernb.activity.domain.port.read;

import java.util.List;

/// 排行榜成就信号只读端口(读既有 leaderboard_rank_snapshot 表)。
public interface LeaderboardAchievementSignalPort {

    /// 用户 id + 历史最佳名次(数值越小名次越好)。
    record UserRank(long userId, int bestRankEver) {
    }

    /// 每个用户历史最佳名次(跨全部 period/metric 取 MIN;30 天清理窗口内的快照,深化稿 §6.3)。
    List<UserRank> bestRankEver();
}
