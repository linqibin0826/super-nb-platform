package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 充值榜单条目,`RechargeReadPort.leaderboard` 按金额降序返回。
///
/// @param name   充值用户的脱敏邮箱(如 ab***@qq.com)
/// @param amount 统计窗口内该用户的充值总额(元)
public record LeaderEntry(String name, BigDecimal amount) {
}
