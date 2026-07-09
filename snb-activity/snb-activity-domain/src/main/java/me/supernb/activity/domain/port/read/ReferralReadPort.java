package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.ReferralInviteEntry;
import me.supernb.activity.domain.model.read.ReferralRechargeEntry;

/// 拉新双榜只读查询端口(infra 委托 snb-sub2api 的 ReferralReadModel 实现)。窗口 [start, end),end 排他。
public interface ReferralReadPort {

    /// 充值榜 Top limit:窗口内经邀请注册的新用户其窗口内充值按邀请人聚合,原始总额降序,name 已脱敏。
    List<ReferralRechargeEntry> rechargeBoard(Instant start, Instant end, BigDecimal cap, int limit);

    /// 人数榜 Top limit:曾开通新人组(group_id=newcomerGroupId)的被邀请人数按邀请人聚合,人数降序,name 已脱敏。
    List<ReferralInviteEntry> inviteBoard(long newcomerGroupId, int limit);
}
