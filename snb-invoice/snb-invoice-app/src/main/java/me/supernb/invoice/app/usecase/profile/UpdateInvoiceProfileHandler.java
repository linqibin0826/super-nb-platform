package me.supernb.invoice.app.usecase.profile;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.invoice.app.usecase.profile.command.UpdateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import org.springframework.stereotype.Service;

/// 改抬头用例:同套字段校验;归属未命中 404。
@Service
public class UpdateInvoiceProfileHandler implements CommandHandler<UpdateInvoiceProfileCommand, Void> {

    private final InvoiceProfileRepository repository;

    /// 构造:注入抬头仓储端口。
    public UpdateInvoiceProfileHandler(InvoiceProfileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Void handle(UpdateInvoiceProfileCommand cmd) {
        var data = ProfileValidator.validate(cmd.data());
        if (!repository.update(cmd.userId(), cmd.profileId(), data)) {
            throw InvoiceException.profileNotFound(cmd.profileId());
        }
        return null;
    }
}
