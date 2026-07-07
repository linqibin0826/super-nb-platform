package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 充值动态条目(name 已脱敏)。
public record RechargeEntry(String name, BigDecimal amount, Instant at) {
}
