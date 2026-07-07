package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 我的中奖记录。相对 [RawDraw] 已 enrich 兑换码状态,面向本人展示,不做脱敏。
///
/// @param amount      中奖金额(元)
/// @param redeemCode  兑换码(安慰奖为 null)
/// @param consolation 是否安慰奖
/// @param createdAt   中奖时刻
/// @param codeStatus  兑换码当前状态(enrich 字段,查询时现取)
/// @param expiresAt   兑换码过期时刻(enrich 字段,查询时现取)
public record MyDrawView(
        BigDecimal amount,
        String redeemCode,
        boolean consolation,
        Instant createdAt,
        String codeStatus,
        Instant expiresAt) {
}
