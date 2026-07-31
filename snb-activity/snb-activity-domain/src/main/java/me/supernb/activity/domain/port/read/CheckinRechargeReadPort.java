package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/// 补给资格窗口充值只读端口(sub2api 库):判定窗口=当月新增真实充值(spec §5.2 决策1),
/// 窗口一律 [monthStart, monthEndExclusive)。
public interface CheckinRechargeReadPort {

    /// 单人窗口内真实充值(元);无流水返回 0。
    BigDecimal monthlyRecharge(long userId, Instant monthStart, Instant monthEndExclusive);

    /// 批量窗口内真实充值(一条 SQL,月度结算批处理用);窗口内无流水的 user 缺席于返回 map。
    Map<Long, BigDecimal> monthlyRecharges(Collection<Long> userIds, Instant monthStart,
            Instant monthEndExclusive);

    /// 历史累计真实充值(元),窗口 [EPOCH, asOf)。返网费的 ¥30 门槛判定用
    /// (spec 2026-07-31-checkin-daily-reward §1.2)。
    ///
    /// 🚨 口径与上面两个方法同源(ZPay 完成单 ∪ 非镜像 balance 兑换码),只是把窗口拉到全历史
    /// ——**绝不能换成 GateRechargeReadPort.totalRecharged()**,那是只算 payment_orders 的
    /// 金票闸机口径,会把闲鱼购码的老客户(真金白银付过钱)判成未达标。
    BigDecimal lifetimeRecharge(long userId, Instant asOf);
}
