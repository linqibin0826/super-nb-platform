package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 榜单条目(name 已脱敏)。
public record LeaderEntry(String name, BigDecimal amount) {
}
