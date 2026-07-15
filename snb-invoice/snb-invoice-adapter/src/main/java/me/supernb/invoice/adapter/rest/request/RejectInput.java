package me.supernb.invoice.adapter.rest.request;

/// 驳回入参。
public record RejectInput(String reason, boolean refundFee) {
}
