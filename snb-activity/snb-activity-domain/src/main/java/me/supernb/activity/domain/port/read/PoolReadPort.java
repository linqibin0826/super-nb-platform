package me.supernb.activity.domain.port.read;

import java.util.List;
import me.supernb.activity.domain.model.read.PoolTier;

/// 奖池查询端口(活动库)。只出份数统计,绝不带出 redeem_code / claimed_by 等敏感列。
public interface PoolReadPort {

    /// 按金额档位统计奖池余量,结果金额升序。
    List<PoolTier> pool(long campaignId);
}
