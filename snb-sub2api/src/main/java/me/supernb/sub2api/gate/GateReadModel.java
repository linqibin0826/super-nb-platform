package me.supernb.sub2api.gate;

import java.math.BigDecimal;

/// sub2api 金票门槛只读读模型(防腐层契约):累计真实充值。
/// 口径=台账「真实付费」标准:只认 payment_orders 中 `status='COMPLETED' AND order_type='balance'`
/// 的 amount 合计;后台赠送/注册金/订阅单一律不算;无记录返回 0。
public interface GateReadModel {

    /// 用户累计真实充值(元)。
    BigDecimal totalRecharged(long userId);
}
