package me.supernb.activity.domain.port.raffle;

import java.util.random.RandomGenerator;
import me.supernb.activity.domain.model.raffle.RaffleDrawSummary;

/// 开奖端口:单事务完成 CAS 抢闸+门槛复核+抽样+奖品归属+留痕(spec §7)。
/// rng 由调用方注入(生产 SecureRandom,测试固定种子)。
public interface RaffleDrawPort {

    /// 对一期执行开奖;CAS 未命中(已开过/非 active)返回 skipped 摘要,不抛异常。
    RaffleDrawSummary drawCampaign(long campaignId, RandomGenerator rng);
}
