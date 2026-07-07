package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 我的中奖记录(已 enrich 兑换码状态,面向本人不脱敏)。
public record MyDrawView(
        BigDecimal amount,
        String redeemCode,
        boolean consolation,
        Instant createdAt,
        String codeStatus,
        Instant expiresAt) {
}
