package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 活动库里的一条原始抽奖记录(未 enrich)。
public record RawDraw(BigDecimal amount, String redeemCode, boolean consolation, Instant createdAt) {
}
