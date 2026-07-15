package me.supernb.invoice.app.usecase.profile.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 建抬头命令;返回新抬头 id(JSON 契约要求字符串)。
public record CreateInvoiceProfileCommand(long userId, ProfileData data) implements Command<String> {
}
