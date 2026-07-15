package me.supernb.invoice.app.usecase.request.dto;

import java.math.BigDecimal;

/// 提交结果(id 字符串化;amount/fee 为申请时刻快照)。
public record CreateInvoiceRequestResult(String id, String requestNo, BigDecimal amount, BigDecimal fee) {
}
