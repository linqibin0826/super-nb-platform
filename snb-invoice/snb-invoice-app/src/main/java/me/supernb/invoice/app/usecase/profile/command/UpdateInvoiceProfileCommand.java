package me.supernb.invoice.app.usecase.profile.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 改抬头命令(全量覆盖)。
public record UpdateInvoiceProfileCommand(long userId, long profileId, ProfileData data) implements Command<Void> {
}
