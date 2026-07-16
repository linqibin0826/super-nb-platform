package me.supernb.invoice.app.usecase.profile;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.invoice.app.usecase.profile.command.CreateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.FeePolicy;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import org.springframework.stereotype.Service;

/// 建抬头用例:字段校验 → 上限守卫(10 条) → 核验章判定(只读缓存) → 落库,返回 id 字符串。
@Service
public class CreateInvoiceProfileHandler implements CommandHandler<CreateInvoiceProfileCommand, String> {

    private final InvoiceProfileRepository repository;
    private final CompanyRegistryPort registry;

    /// 构造:注入抬头仓储端口与核验端口。
    public CreateInvoiceProfileHandler(InvoiceProfileRepository repository, CompanyRegistryPort registry) {
        this.repository = repository;
        this.registry = registry;
    }

    @Override
    public String handle(CreateInvoiceProfileCommand cmd) {
        var data = ProfileValidator.validate(cmd.data());
        if (repository.countByUser(cmd.userId()) >= FeePolicy.MAX_PROFILES) {
            throw InvoiceException.profileLimitReached(FeePolicy.MAX_PROFILES);
        }
        return String.valueOf(repository.create(cmd.userId(), data, ProfileStamp.evaluate(registry, data)));
    }
}
