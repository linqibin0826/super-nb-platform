package me.supernb.sub2api.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// sub2api 开票只读读模型(防腐层契约):可开票订单口径 = `order_type='balance' AND
/// status='COMPLETED'`(2026-07-15 站长拍板,兑换码不算);余额/邮箱读 users 表。
///
/// ⚠️ emailsByIds 返回**未脱敏完整邮箱**——唯一合法消费方是发票管理后台(admin 身份核对
/// 用户,与 sub2api 自家 admin 面板同权级);任何公开信息流一律走 RechargeReadModel 的脱敏口径。
public interface InvoiceOrderReadModel {

    /// 一笔已完成余额充值单。orderNo 优先商户单号 out_trade_no,空则回退 id 文本。
    record OrderRow(long id, String orderNo, BigDecimal amount, Instant completedAt) {
    }

    /// 用户全部已完成余额充值单(完成时间倒序)。
    List<OrderRow> completedBalanceOrders(long userId);

    /// 用户当前站内余额;查无此人返回 0。
    BigDecimal balanceOf(long userId);

    /// 批量取完整邮箱;查无对应记录的 id 不出现在返回 map 中。
    Map<Long, String> emailsByIds(Collection<Long> userIds);
}
