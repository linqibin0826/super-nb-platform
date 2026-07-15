package me.supernb.invoice.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;

/// 一笔可开票充值订单(来自 sub2api 只读源;申请时快照进 invoice_request_order)。
public record OrderLine(long orderId, String orderNo, BigDecimal amount, Instant completedAt) {
}
