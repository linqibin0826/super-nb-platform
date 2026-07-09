package me.supernb.activity.domain.port.draw;

import java.util.List;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.read.RawDraw;
import me.supernb.activity.domain.model.read.RawWinner;

/// 抽奖端口(活动库)。drawFor 的实现必须在 per-user 串行化(advisory lock)+ 单个事务内
/// 完成,保证并发下不超额发放。
public interface DrawPort {

    /// 原子抽一次:锁定该用户 → 现查剩余次数 → 领槽或记安慰奖。无剩余次数时抛
    /// NoDrawsLeftException。
    DrawResult drawFor(Campaign campaign, long userId);

    /// 原子批量抽奖:同一事务、一把 advisory lock 内抽 min(剩余, BATCH_MAX) 次,逐张返回结果。
    /// 无剩余次数时抛 NoDrawsLeftException。次数上限服务端硬定,无客户端入参。
    List<DrawResult> drawAllFor(Campaign campaign, long userId);

    /// 该活动下这个用户的已抽次数。
    int countDraws(long campaignId, long userId);

    /// 本人在本活动的中奖历史(含安慰奖),时间倒序的原始行,兑换码状态未 enrich。
    List<RawDraw> myRawDraws(long campaignId, long userId);

    /// 最近的真实中奖(排除安慰奖),时间倒序,仅 userId + 金额,邮箱未 enrich。
    List<RawWinner> recentRealWinners(long campaignId, int limit);
}
