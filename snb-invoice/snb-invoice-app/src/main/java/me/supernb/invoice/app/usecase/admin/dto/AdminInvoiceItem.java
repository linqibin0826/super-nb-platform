package me.supernb.invoice.app.usecase.admin.dto;

import me.supernb.invoice.domain.model.read.AdminInvoiceRow;

/// 管理端列表项 = 本库行 + 完整邮箱(admin 核对身份用)。
public record AdminInvoiceItem(AdminInvoiceRow row, String email) {
}
