package me.supernb.invoice.app.usecase.registry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.FeePolicy;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/// 抬头核验查询服务:入参把门 → 开票资格闸(累计充值 ≥ 最低开票线,防未达门槛的账号
/// 白嫖付费接口) → 日配额守卫 → 走端口(缓存优先)。资格闸放在配额之前,不合格用户
/// 不占全站配额。配额是付费接口的烧钱保护:单用户日限防脚本刷、全站日限兜底封顶;
/// 内存自然日计数,重启清零可接受(保护不是计费,且端口侧还有缓存挡重复查询)。
@Service
public class RegistryLookupService {

    private final CompanyRegistryPort registry;
    private final BillableOrderReadPort billableOrders;
    private final int userDaily;
    private final int globalDaily;

    private final Map<Long, Integer> userCounts = new HashMap<>();
    private LocalDate day = LocalDate.now();
    private int globalCount;

    /// 构造:注入核验端口、充值只读端口与两级日配额(`invoice.registry.*`,默认单用户 10/全站 40)。
    public RegistryLookupService(CompanyRegistryPort registry, BillableOrderReadPort billableOrders,
            @Value("${invoice.registry.user-daily:10}") int userDaily,
            @Value("${invoice.registry.global-daily:40}") int globalDaily) {
        this.registry = registry;
        this.billableOrders = billableOrders;
        this.userDaily = userDaily;
        this.globalDaily = globalDaily;
    }

    /// 按企业名称核验;empty = 供应商查无此企业。资格未达标/超配额 422/429、通道不可用 422。
    public Optional<CompanyRecord> lookup(long userId, String enterpriseName) {
        String name = enterpriseName == null ? "" : enterpriseName.strip();
        if (name.length() < 4 || name.length() > 100) {
            throw InvoiceException.invalidInput("企业名称长度不合法");
        }
        requireInvoiceEligible(userId);
        consume(userId);
        return registry.lookup(name);
    }

    /// 资格 = 累计已完成余额充值 ≥ 最低开票线(与提交申请同口径,占用与否不影响——
    /// 开过票的老客户仍是合格用户)。
    private void requireInvoiceEligible(long userId) {
        BigDecimal recharged = billableOrders.completedOrders(userId).stream()
                .map(OrderLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (recharged.compareTo(FeePolicy.MIN_TOTAL) < 0) {
            throw InvoiceException.registryRequiresRecharge(FeePolicy.MIN_TOTAL);
        }
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
