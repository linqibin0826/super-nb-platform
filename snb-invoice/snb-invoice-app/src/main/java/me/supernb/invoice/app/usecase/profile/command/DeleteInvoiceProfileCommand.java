package me.supernb.invoice.app.usecase.profile.command;

import dev.linqibin.commons.cqrs.Command;

/// 删抬头命令(硬删;申请单持有独立快照不受影响)。
public record DeleteInvoiceProfileCommand(long userId, long profileId) implements Command<Void> {
}
