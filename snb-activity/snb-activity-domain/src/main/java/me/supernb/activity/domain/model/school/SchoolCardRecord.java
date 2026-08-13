package me.supernb.activity.domain.model.school;

/// 包机邀请卡(玩法 v2,一人一张):tier=已领档位(1=Go/2=Plus/3=ProLite/4=Pro),
/// subscriptionId=sub2api 订阅 id(重置额度调用要用;升档换组后随之更新),
/// resetsUsed=重置银行已消耗次数(获得侧从合格人数推导,不落库)。
public record SchoolCardRecord(long id, long userId, int tier, long subscriptionId, int resetsUsed) {
}
