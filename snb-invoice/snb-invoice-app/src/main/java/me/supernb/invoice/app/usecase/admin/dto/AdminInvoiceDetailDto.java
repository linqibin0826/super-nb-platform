package me.supernb.invoice.app.usecase.admin.dto;

import me.supernb.invoice.domain.model.read.InvoiceRequestDetail;

/// 管理端详情 = 本库详情 + 完整邮箱。
public record AdminInvoiceDetailDto(InvoiceRequestDetail detail, String email) {
}
