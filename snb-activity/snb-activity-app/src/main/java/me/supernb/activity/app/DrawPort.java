package me.supernb.activity.app;

import java.util.List;
import me.supernb.activity.domain.Campaign;
import me.supernb.activity.domain.DrawResult;

/// 抽奖端口(活动库)。drawFor 的实现必须在 per-user 串行化(advisory lock)+ 事务内完成,
/// 保证并发下不超额发放。
public interface DrawPort {

    /// 原子抽一次:锁定该用户 → 现查剩余次数 → 领槽/记安慰奖。无剩余抛 NoDrawsLeftException。
    DrawResult drawFor(Campaign campaign, long userId);

    /// 某活动+某用户的已抽次数。
    int countDraws(long campaignId, long userId);

    /// 本人在本活动的中奖历史(含安慰奖,时间倒序原始行,未 enrich 兑换码状态)。
    List<ActivityDto.RawDraw> myRawDraws(long campaignId, long userId);

    /// 最近的真实中奖(排除安慰奖,时间倒序,仅 userId+金额,未 enrich 邮箱)。
    List<ActivityDto.RawWinner> recentRealWinners(long campaignId, int limit);
}
