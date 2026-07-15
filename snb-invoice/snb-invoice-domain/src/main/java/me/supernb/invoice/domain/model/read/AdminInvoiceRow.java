package me.supernb.invoice.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.invoice.domain.model.InvoiceStatus;

/// 管理端列表行(email 由 app 层从 sub2api 只读源补齐,本行只有本库字段)。
public record AdminInvoiceRow(long id, String requestNo, long userId, BigDecimal amount, BigDecimal fee,
                              InvoiceStatus status, Instant createdAt) {
}
