package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 奖池档位余量。
public record PoolTier(BigDecimal amount, int total, int available) {
}
