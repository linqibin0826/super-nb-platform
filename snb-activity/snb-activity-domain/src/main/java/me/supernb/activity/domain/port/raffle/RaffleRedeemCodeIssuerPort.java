package me.supernb.activity.domain.port.raffle;

import java.util.List;

/// 兑换码批量签发端口(唯一实现来源是 sub2api,见 snb-activity-infra 的桥接适配器)。
public interface RaffleRedeemCodeIssuerPort {

    /// 签发 count 张 subscription 类型兑换码,返回明文码值(按生成顺序)。
    List<String> issue(long groupId, int validityDays, int count);
}
