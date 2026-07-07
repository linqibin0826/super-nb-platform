package me.supernb.activity.app;

import java.util.List;

/// 奖池查询端口(活动库)。只出份数统计,绝不带出 redeem_code / claimed_by 等敏感列。
public interface PoolPort {

    /// 按金额档位统计余量(金额升序)。
    List<ActivityDto.PoolTier> pool(long campaignId);
}
