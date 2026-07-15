package me.supernb.invoice.domain.port.read;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import me.supernb.invoice.domain.model.read.OrderLine;

/// sub2api 主库只读端口:可开票订单口径 = payment_orders 中 order_type='balance' AND
/// status='COMPLETED'(兑换码不算,2026-07-15 站长拍板);余额/邮箱同源。
public interface BillableOrderReadPort {

    /// 用户全部已完成余额充值单(完成时间倒序)。
    List<OrderLine> completedOrders(long userId);

    /// 用户当前站内余额(1:1 计价);查无此人返回 0。
    BigDecimal balanceOf(long userId);

    /// 批量取完整邮箱——仅限管理端核对身份消费,绝不进公开流。
    Map<Long, String> emailsByIds(Collection<Long> userIds);
}
