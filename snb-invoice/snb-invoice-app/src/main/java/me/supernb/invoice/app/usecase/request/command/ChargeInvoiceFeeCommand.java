package me.supernb.invoice.app.usecase.request.command;

import dev.linqibin.commons.cqrs.Command;

/// 管理员扣手续费并受理命令;返回受理后的状态名(幂等重放也返回现状)。
public record ChargeInvoiceFeeCommand(long requestId) implements Command<String> {
}
