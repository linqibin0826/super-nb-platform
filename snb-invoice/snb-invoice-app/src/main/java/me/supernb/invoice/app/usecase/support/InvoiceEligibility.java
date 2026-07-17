package me.supernb.invoice.app.usecase.support;

import java.math.BigDecimal;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.FeePolicy;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;

/// 开票资格闸:累计已完成余额充值 ≥ 最低开票线(与提交申请同口径,订单被占用与否不影响
/// ——开过票的老客户仍合格)。核验/AI 识别两个付费能力共用,闸一律放在配额消耗之前。
/// 管理员豁免充值门槛(站长自测/运营排查,2026-07-17 拍板),但日配额照常计——
/// 配额守的是聚美/LLM 的真金白银,管理员误操作同样烧钱。
public final class InvoiceEligibility {

    private InvoiceEligibility() {
    }

    /// 未达标抛 422(registryRequiresRecharge);admin=true 直接放行。
    public static void requireRecharged(BillableOrderReadPort billableOrders, long userId, boolean admin) {
        if (admin) {
            return;
        }
        BigDecimal recharged = billableOrders.completedOrders(userId).stream()
                .map(OrderLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (recharged.compareTo(FeePolicy.MIN_TOTAL) < 0) {
            throw InvoiceException.registryRequiresRecharge(FeePolicy.MIN_TOTAL);
        }
    }
}
