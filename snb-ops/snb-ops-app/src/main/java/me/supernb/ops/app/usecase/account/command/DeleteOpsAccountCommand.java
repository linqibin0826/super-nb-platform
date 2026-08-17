package me.supernb.ops.app.usecase.account.command;

import dev.linqibin.commons.cqrs.Command;

/// 删账号命令(名下有订阅时拒绝)。
public record DeleteOpsAccountCommand(long id) implements Command<Void> {
}
