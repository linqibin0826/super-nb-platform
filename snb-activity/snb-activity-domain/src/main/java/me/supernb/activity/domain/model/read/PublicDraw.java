package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 公开中奖信息流条目(name 已脱敏)。
public record PublicDraw(String name, BigDecimal amount) {
}
