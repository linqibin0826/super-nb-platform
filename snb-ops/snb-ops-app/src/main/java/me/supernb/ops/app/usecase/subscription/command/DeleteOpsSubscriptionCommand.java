package me.supernb.ops.app.usecase.subscription.command;

import dev.linqibin.commons.cqrs.Command;

/// 删订阅命令。
public record DeleteOpsSubscriptionCommand(long id) implements Command<Void> {
}
