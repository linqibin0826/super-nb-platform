package me.supernb.invoice.app.usecase.registry;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/// 抬头核验查询服务:入参把门 → 日配额守卫 → 走端口(缓存优先)。配额是付费接口的烧钱保护:
/// 单用户日限防脚本刷、全站日限兜底封顶;内存自然日计数,重启清零可接受(保护不是计费,
/// 且端口侧还有缓存挡重复查询)。缓存命中也计配额——语义简单,且命中本来免费、多算无伤。
@Service
public class RegistryLookupService {

    private final CompanyRegistryPort registry;
    private final int userDaily;
    private final int globalDaily;

    private final Map<Long, Integer> userCounts = new HashMap<>();
    private LocalDate day = LocalDate.now();
    private int globalCount;

    /// 构造:注入核验端口与两级日配额(`invoice.registry.*`,默认单用户 10/全站 40)。
    public RegistryLookupService(CompanyRegistryPort registry,
            @Value("${invoice.registry.user-daily:10}") int userDaily,
            @Value("${invoice.registry.global-daily:40}") int globalDaily) {
        this.registry = registry;
        this.userDaily = userDaily;
        this.globalDaily = globalDaily;
    }

    /// 按企业名称核验;empty = 供应商查无此企业。超配额抛 429、通道不可用由端口抛 422。
    public Optional<CompanyRecord> lookup(long userId, String enterpriseName) {
        String name = enterpriseName == null ? "" : enterpriseName.strip();
        if (name.length() < 4 || name.length() > 100) {
            throw InvoiceException.invalidInput("企业名称长度不合法");
        }
        consume(userId);
        return registry.lookup(name);
    }

    private synchronized void consume(long userId) {
        LocalDate today = LocalDate.now();
        if (!today.equals(day)) {
            day = today;
            globalCount = 0;
            userCounts.clear();
        }
        int mine = userCounts.getOrDefault(userId, 0);
        if (mine >= userDaily || globalCount >= globalDaily) {
            throw InvoiceException.registryQuotaExceeded();
        }
        userCounts.put(userId, mine + 1);
        globalCount++;
    }
}
