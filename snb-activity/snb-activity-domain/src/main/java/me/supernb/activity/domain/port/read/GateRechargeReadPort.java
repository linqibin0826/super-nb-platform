package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;

/// 金票门槛用的累计真实充值查询端口(sub2api 主库只读源)。
/// 口径=台账「真实付费」标准:只认 payment_orders 中 COMPLETED 的 balance 单,
/// 后台赠送/注册金一律不算;无记录返回 0。
public interface GateRechargeReadPort {

    /// 用户累计真实充值(元)。
    BigDecimal totalRecharged(long userId);
}
