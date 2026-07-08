package me.supernb.activity.domain.model.read;

import java.time.Instant;

/// 兑换码状态(读侧形态),由 `RechargeReadPort.codeStatuses` 批量返回,用于 enrich 中奖记录。
///
/// @param status    兑换码当前状态字符串(取自 sub2api redeem_codes 表)
/// @param expiresAt 过期时刻;该码不设过期时为 null
public record CodeStatus(String status, Instant expiresAt) {
}
