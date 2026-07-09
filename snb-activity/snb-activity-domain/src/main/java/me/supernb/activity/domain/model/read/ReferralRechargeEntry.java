package me.supernb.activity.domain.model.read;

import java.math.BigDecimal;

/// 拉新充值榜条目,`ReferralReadPort.rechargeBoard` 按原始总额降序返回。
///
/// @param name   邀请人脱敏邮箱(如 ab***@qq.com)
/// @param total  被邀请新用户窗口内充值原始合计(元,排序依据)
/// @param capped 匹配奖励封顶后金额(min(total, cap))
public record ReferralRechargeEntry(String name, BigDecimal total, BigDecimal capped) {
}
