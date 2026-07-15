package me.supernb.invoice.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.invoice.domain.model.InvoiceStatus;

/// 「我的申请」列表行。
public record InvoiceRequestView(long id, String requestNo, BigDecimal amount, BigDecimal fee,
                                 InvoiceStatus status, String profileTitle, String remark,
                                 String rejectReason, Instant createdAt, Instant issuedAt) {
}
