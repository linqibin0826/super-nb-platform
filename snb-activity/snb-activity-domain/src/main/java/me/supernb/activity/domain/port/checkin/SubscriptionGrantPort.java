package me.supernb.activity.domain.port.checkin;

import java.util.List;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;

/// 补给资格订阅发放端口(委托 sub2api admin bulk-assign,spec §7.4 唯一安全通道——
/// 绝不调用 redeem-codes/generate 或 admin 普通余额发放)。
public interface SubscriptionGrantPort {

    /// 批量分配订阅;单个用户失败不中断整批,调用方按返回的 statuses 逐个处理。
    SubscriptionGrantOutcome bulkGrant(List<Long> userIds, long groupId, int validityDays, String notes);

    /// 单发分配订阅并返回订阅 id(包机邀请卡开卡/升档:id 落库供 reset-quota 调用)。
    long assign(long userId, long groupId, int validityDays, String notes);

    /// 吊销订阅(升档换组后收走旧档卡)。
    void revoke(long subscriptionId);

    /// 重置订阅额度(三窗全清,总额卡卡面回满——重置银行的下游动作)。
    void resetQuota(long subscriptionId);
}
