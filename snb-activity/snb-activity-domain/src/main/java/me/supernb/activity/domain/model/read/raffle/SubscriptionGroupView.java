package me.supernb.activity.domain.model.read.raffle;

/// 可生成兑换码的 sub2api 订阅分组(管理端下拉选择用,只含 id 与名称)。
public record SubscriptionGroupView(long id, String name) {}
