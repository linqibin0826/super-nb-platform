package me.supernb.invoice.app.usecase.profile;

import java.time.Instant;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 核验章判定(create/update 共用):企业抬头的 名称+税号 与缓存中的官方记录逐字相符才盖章。
/// 章的语义=「与官方开票资料一致」,地址/开户行不参与判定(普票选填、格式天然有出入)。
/// 只读缓存绝不触发付费调用——缓存来自用户刚点过的「核验」,没点过就是没章,保存不烧钱。
final class ProfileStamp {

    private ProfileStamp() {
    }

    /// 返回盖章时刻;不满足(个人抬头/无税号/缓存无记录/字段对不上)返回 null。
    static Instant evaluate(CompanyRegistryPort registry, ProfileData data) {
        if (data.type() != ProfileType.COMPANY || data.taxNo() == null) {
            return null;
        }
        return registry.cached(data.title())
                .filter(r -> data.title().equals(r.name()) && data.taxNo().equalsIgnoreCase(r.taxNo()))
                .map(r -> Instant.now())
                .orElse(null);
    }
}
