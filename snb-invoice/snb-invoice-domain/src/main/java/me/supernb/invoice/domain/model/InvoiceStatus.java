package me.supernb.invoice.domain.model;

/// 发票申请状态机:PENDING →(扣费)→ INVOICING →(传 PDF)→ ISSUED;
/// PENDING 可被用户撤回(CANCELLED)或管理员驳回;INVOICING 可驳回(可选退费)。
public enum InvoiceStatus {
    PENDING, INVOICING, ISSUED, REJECTED, CANCELLED
}
