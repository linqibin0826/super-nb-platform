package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;

/// 补给记录成就只读端口:全量充值现查透传不落表(深化稿 §6.1)。
public interface AchievementRechargeReadPort {

    /// 用户全部历史真实充值(元)。
    BigDecimal totalRecharged(long userId);

    /// 是否连续 3 个自然月(本月、上月、上上月)均有真实充值 > 0。
    boolean hasThreeConsecutiveMonthsOfRecharge(long userId);
}
