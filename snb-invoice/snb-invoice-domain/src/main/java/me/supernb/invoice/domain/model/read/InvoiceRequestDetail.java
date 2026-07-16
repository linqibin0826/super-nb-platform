package me.supernb.invoice.domain.model.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.ProfileType;

/// 管理端申请详情:申请单全字段 + 抬头快照(含提交时核验章) + 订单明细。
public record InvoiceRequestDetail(long id, String requestNo, long userId, BigDecimal amount, BigDecimal fee,
                                   InvoiceStatus status, ProfileType profileType, String profileTitle,
                                   String profileTaxNo, String profileRegAddress, String profileRegPhone,
                                   String profileBankName, String profileBankAccount, Instant profileVerifiedAt,
                                   String remark, String rejectReason, Instant feeChargedAt, Instant issuedAt,
                                   Instant createdAt, List<OrderLine> orders) {
}
