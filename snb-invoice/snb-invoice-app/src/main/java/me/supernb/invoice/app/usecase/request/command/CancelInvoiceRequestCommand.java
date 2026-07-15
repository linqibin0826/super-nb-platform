package me.supernb.invoice.app.usecase.request.command;

import dev.linqibin.commons.cqrs.Command;

/// 用户撤回申请命令(仅 PENDING)。
public record CancelInvoiceRequestCommand(long userId, long requestId) implements Command<Void> {
}
