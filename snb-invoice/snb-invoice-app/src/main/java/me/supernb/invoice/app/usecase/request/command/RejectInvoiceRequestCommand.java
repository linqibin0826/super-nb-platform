package me.supernb.invoice.app.usecase.request.command;

import dev.linqibin.commons.cqrs.Command;

/// 管理员驳回命令;refundFee 仅在 INVOICING 驳回时有意义(PENDING 本就没扣过)。
public record RejectInvoiceRequestCommand(long requestId, String reason, boolean refundFee) implements Command<Void> {
}
