package me.supernb.activity.domain;

import java.math.BigDecimal;

/// 单次抽奖结果。安慰奖 redeemCode 为 null(人工发放)。
///
/// @param amount      中奖金额(元)
/// @param redeemCode  兑换码(安慰奖为 null)
/// @param consolation 是否安慰奖
public record DrawResult(BigDecimal amount, String redeemCode, boolean consolation) {

    public static DrawResult prize(BigDecimal amount, String redeemCode) {
        return new DrawResult(amount, redeemCode, false);
    }

    public static DrawResult consolation(BigDecimal amount) {
        return new DrawResult(amount, null, true);
    }
}
