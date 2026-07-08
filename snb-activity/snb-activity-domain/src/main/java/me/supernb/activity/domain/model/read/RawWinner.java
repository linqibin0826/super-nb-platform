package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 一条真实中奖记录,未 enrich(仅 userId + 金额,不含邮箱/兑换码;安慰奖已在
/// `DrawPort.recentRealWinners` 查询源头被过滤掉,不会出现在这里)。
///
/// @param userId 中奖用户 id
/// @param amount 中奖金额(元)
public record RawWinner(long userId, BigDecimal amount) {
}
