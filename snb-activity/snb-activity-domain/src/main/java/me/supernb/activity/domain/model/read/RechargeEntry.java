package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 充值动态条目,RechargeReadPort.recentRecharges 按完成时间倒序返回。
///
/// @param name   充值用户的脱敏邮箱(如 ab***@qq.com)
/// @param amount 单笔充值金额(元)
/// @param at     该笔充值完成时刻
public record RechargeEntry(String name, BigDecimal amount, Instant at) {
}
