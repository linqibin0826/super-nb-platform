package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 一条真实中奖记录(未 enrich,仅 userId+金额)。
public record RawWinner(long userId, BigDecimal amount) {
}
