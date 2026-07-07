package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 活动库里的一条原始抽奖记录,未 enrich(兑换码状态需另调 RechargeReadPort.codeStatuses)。
///
/// @param amount      中奖/安慰奖金额(元)
/// @param redeemCode  兑换码(安慰奖为 null)
/// @param consolation 是否安慰奖
/// @param createdAt   抽奖时刻
public record RawDraw(BigDecimal amount, String redeemCode, boolean consolation, Instant createdAt) {
}
