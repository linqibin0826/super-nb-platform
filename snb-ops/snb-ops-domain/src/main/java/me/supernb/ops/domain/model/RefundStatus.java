package me.supernb.ops.domain.model;

/// 退款五态:无需退款/待申诉/申诉中/已到账/被拒;PENDING+APPEALING=未结案(进看板跟进)。
public enum RefundStatus {
    NONE, PENDING, APPEALING, REFUNDED, REJECTED
}
