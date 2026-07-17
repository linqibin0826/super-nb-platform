package me.supernb.invoice.app.usecase.registry;

import java.util.Optional;
import me.supernb.invoice.app.usecase.support.DailyQuota;
import me.supernb.invoice.app.usecase.support.InvoiceEligibility;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/// 抬头核验查询服务:入参把门 → 开票资格闸(防未达门槛的账号白嫖付费接口,不合格不占
/// 全站配额) → 日配额守卫 → 走端口(缓存优先)。缓存命中也计配额——语义简单,命中本来
/// 免费、多算无伤。
@Service
public class RegistryLookupService {

    private final CompanyRegistryPort registry;
    private final BillableOrderReadPort billableOrders;
    private final DailyQuota quota;

    /// 构造:注入核验端口、充值只读端口与两级日配额(`invoice.registry.*`,默认单用户 10/全站 40)。
    public RegistryLookupService(CompanyRegistryPort registry, BillableOrderReadPort billableOrders,
            @Value("${invoice.registry.user-daily:10}") int userDaily,
            @Value("${invoice.registry.global-daily:40}") int globalDaily) {
        this.registry = registry;
        this.billableOrders = billableOrders;
        this.quota = new DailyQuota(userDaily, globalDaily);
    }

    /// 按企业名称核验;empty = 供应商查无此企业。资格未达标/超配额 422/429、通道不可用 422。
    /// admin 豁免充值门槛(配额照常计,见 InvoiceEligibility)。
    public Optional<CompanyRecord> lookup(long userId, boolean admin, String enterpriseName) {
        String name = enterpriseName == null ? "" : enterpriseName.strip();
        if (name.length() < 4 || name.length() > 100) {
            throw InvoiceException.invalidInput("企业名称长度不合法");
        }
        InvoiceEligibility.requireRecharged(billableOrders, userId, admin);
        quota.consume(userId, InvoiceException::registryQuotaExceeded);
        return registry.lookup(name);
    }
}
