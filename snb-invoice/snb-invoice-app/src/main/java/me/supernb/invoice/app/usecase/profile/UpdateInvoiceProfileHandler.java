package me.supernb.invoice.app.usecase.profile;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.Instant;
import java.util.Objects;
import me.supernb.invoice.app.usecase.profile.command.UpdateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.StoredProfile;
import org.springframework.stereotype.Service;

/// 改抬头用例:同套字段校验;归属未命中 404。核验章跟内容走——名称/税号没动就保留原章,
/// 动了就按缓存重新判定(对不上即掉章),防「先核验再偷改」把章变成假章。
@Service
public class UpdateInvoiceProfileHandler implements CommandHandler<UpdateInvoiceProfileCommand, Void> {

    private final InvoiceProfileRepository repository;
    private final CompanyRegistryPort registry;

    /// 构造:注入抬头仓储端口与核验端口。
    public UpdateInvoiceProfileHandler(InvoiceProfileRepository repository, CompanyRegistryPort registry) {
        this.repository = repository;
        this.registry = registry;
    }

    @Override
    public Void handle(UpdateInvoiceProfileCommand cmd) {
        var data = ProfileValidator.validate(cmd.data());
        var existing = repository.find(cmd.userId(), cmd.profileId())
                .orElseThrow(() -> InvoiceException.profileNotFound(cmd.profileId()));
        if (!repository.update(cmd.userId(), cmd.profileId(), data, stampFor(existing, data))) {
            throw InvoiceException.profileNotFound(cmd.profileId());
        }
        return null;
    }

    /// 章覆盖的字段(类型/名称/税号)全没动 → 保留原章(缓存过期不该冤枉没改的抬头);
    /// 否则按当前缓存重判(编辑非章字段如开户行,重判也会因名称税号未变而保住章)。
    private Instant stampFor(StoredProfile existing, ProfileData next) {
        ProfileData prev = existing.data();
        boolean stampFieldsUntouched = prev.type() == next.type()
                && prev.type() == ProfileType.COMPANY
                && Objects.equals(prev.title(), next.title())
                && Objects.equals(prev.taxNo(), next.taxNo());
        return stampFieldsUntouched ? existing.verifiedAt() : ProfileStamp.evaluate(registry, next);
    }
}
