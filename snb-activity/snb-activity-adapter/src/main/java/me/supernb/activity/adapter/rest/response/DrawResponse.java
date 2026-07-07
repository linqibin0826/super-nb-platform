package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;

/// 抽奖结果响应。
public record DrawResponse(BigDecimal amount, String redeemCode, boolean consolation) {
}
