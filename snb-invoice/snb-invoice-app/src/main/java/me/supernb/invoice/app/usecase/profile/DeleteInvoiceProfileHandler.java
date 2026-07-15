package me.supernb.invoice.app.usecase.profile;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.invoice.app.usecase.profile.command.DeleteInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import org.springframework.stereotype.Service;

/// 删抬头用例:归属未命中 404。
@Service
public class DeleteInvoiceProfileHandler implements CommandHandler<DeleteInvoiceProfileCommand, Void> {

    private final InvoiceProfileRepository repository;

    /// 构造:注入抬头仓储端口。
    public DeleteInvoiceProfileHandler(InvoiceProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Void handle(DeleteInvoiceProfileCommand cmd) {
        if (!repository.delete(cmd.userId(), cmd.profileId())) {
            throw InvoiceException.profileNotFound(cmd.profileId());
        }
        return null;
    }
}
