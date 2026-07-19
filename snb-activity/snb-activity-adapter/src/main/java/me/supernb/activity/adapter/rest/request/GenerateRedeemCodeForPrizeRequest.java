package me.supernb.activity.adapter.rest.request;

/// 逐行生成兑换码请求:分组 + 有效天数(数量恒 1,回填到路径指定的奖品行)。
public record GenerateRedeemCodeForPrizeRequest(long groupId, int validityDays) {}
