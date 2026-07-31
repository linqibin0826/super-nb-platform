package me.supernb.activity.domain.port.checkin;

import java.math.BigDecimal;

/// 余额发放端口(经 sub2api admin API)。
///
/// 🚨 **只走 admin API,绝不直写库**——复用上游负余额保护、幂等键、自动清计费缓存与
/// `admin_balance` 审计;且不产生 `payment_orders`,发出去的钱不会回流成「真实充值」
/// 去解锁金票闸机 / 疯四桶 / 加时门槛 / 返网费自己的 ¥30 门槛
/// (spec 2026-07-31-checkin-daily-reward §1.3——白送的钱不能变成下一轮白送的入场券)。
public interface BalanceGrantPort {

    /// 给用户加余额;上游拒绝或传输故障一律抛异常,由调用方转台账 failed 状态。
    ///
    /// @param notes 必须含业务单号使上游幂等键可辨识。本功能用 `checkin-daily-{yyyy-MM-dd}`
    ///              固定模板,**不含时间戳**——含时间戳会让幂等键漂移
    void grant(long userId, BigDecimal amountCny, String notes);
}
