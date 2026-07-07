package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;

/// 抽奖结果响应,对应 `POST /activity/v1/draw`。字段与领域层的抽奖结果一一对应、直接复制,不做二次映射。
///
/// @param amount      中奖金额(元)
/// @param redeemCode  兑换码(安慰奖为 null,人工发放)
/// @param consolation 是否安慰奖
public record DrawResponse(BigDecimal amount, String redeemCode, boolean consolation) {
}
