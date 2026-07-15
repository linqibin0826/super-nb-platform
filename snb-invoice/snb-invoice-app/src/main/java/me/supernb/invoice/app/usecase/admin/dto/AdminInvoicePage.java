package me.supernb.invoice.app.usecase.admin.dto;

import java.util.List;

/// 管理端分页信封。
public record AdminInvoicePage(List<AdminInvoiceItem> items, long total) {
}
