package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 公开中奖信息流条目。相对 [RawWinner] 已用脱敏邮箱 enrich 出展示名;安慰奖不出现在这里,
/// DrawPort.recentRealWinners 在查询源头就把 consolation 过滤掉了。
///
/// @param name   中奖用户的脱敏邮箱(如 ab***@qq.com)
/// @param amount 中奖金额(元)
public record PublicDraw(String name, BigDecimal amount) {
}
