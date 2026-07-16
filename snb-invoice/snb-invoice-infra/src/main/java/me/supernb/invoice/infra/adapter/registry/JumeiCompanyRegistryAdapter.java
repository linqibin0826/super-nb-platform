package me.supernb.invoice.infra.adapter.registry;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/// [CompanyRegistryPort] 实现:聚美智数「企业开票信息查询」(阿里云云市场,APPCODE 简单鉴权,
/// `POST /invoice/title/query` form 传 enterpriseName)。付费接口的省钱三件套:
/// 查得缓存 30 天(注册信息几乎不变)、查无负缓存 10 分钟(挡同名重试连击)、LRU 500 条防涨。
/// appcode 未配置(env `JUMEI_INVOICE_APPCODE` 缺席)时 lookup 报 registryUnavailable、
/// cached 恒空——上下文照常起,核验功能整体静默关闭。
@Component
public class JumeiCompanyRegistryAdapter implements CompanyRegistryPort {

    private static final Duration POSITIVE_TTL = Duration.ofDays(30);
    private static final Duration NEGATIVE_TTL = Duration.ofMinutes(10);
    private static final int MAX_ENTRIES = 500;

    /// 供应商把电话直接拼在地址尾巴上(专票四要素习惯):尾部 ≥8 位的数字/横线/空格串按电话拆走。
    private static final Pattern TRAILING_PHONE = Pattern.compile("([0-9][0-9\\- ()]{6,}[0-9])\\s*$");

    /// 缓存条目:record=null 表示「供应商明确查无」的负缓存。
    private record CacheEntry(CompanyRecord record, Instant at) {
    }

    /// 供应商响应信封(Jackson 对未知字段宽容,只取所需)。
    record JumeiResponse(Boolean success, JumeiData data) {
    }

    /// 供应商数据体:address 为地址+电话混排原文。
    record JumeiData(String enterpriseName, String taxNo, String address, String bankName, String bankNo) {
    }

    private final RestClient rest;
    private final String appcode;
    private final Map<String, CacheEntry> cache = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    /// 构造:appcode 为空则不建 HTTP 客户端(功能关闭);超时收紧(连 5s/读 8s),
    /// 核验挂了不能拖住保存链路。
    public JumeiCompanyRegistryAdapter(
            @Value("${invoice.registry.base-url:https://jminvoicv3.market.alicloudapi.com}") String baseUrl,
            @Value("${invoice.registry.appcode:}") String appcode) {
        this.appcode = appcode == null ? "" : appcode.strip();
        if (this.appcode.isEmpty()) {
            this.rest = null;
        } else {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
            factory.setReadTimeout(Duration.ofSeconds(8));
            this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        }
    }

    @Override
    public Optional<CompanyRecord> lookup(String enterpriseName) {
        CacheEntry hit = freshEntry(enterpriseName);
        if (hit != null) {
            return Optional.ofNullable(hit.record());
        }
        if (rest == null) {
            throw InvoiceException.registryUnavailable("核验通道未配置");
        }
        JumeiResponse resp = call(enterpriseName);
        // success≠true 或 data 缺税号一律按「查无」处理(负缓存短 TTL 兜底);供应商若把自身
        // 故障也收在 success=false,10 分钟后即自愈,不值得为区分再烧一次试探调用。
        CompanyRecord record = null;
        if (resp != null && Boolean.TRUE.equals(resp.success()) && resp.data() != null
                && notBlank(resp.data().taxNo())) {
            record = toRecord(resp.data());
        }
        cache.put(enterpriseName, new CacheEntry(record, Instant.now()));
        return Optional.ofNullable(record);
    }

    @Override
    public Optional<CompanyRecord> cached(String enterpriseName) {
        CacheEntry hit = freshEntry(enterpriseName);
        return hit == null ? Optional.empty() : Optional.ofNullable(hit.record());
    }

    private JumeiResponse call(String enterpriseName) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("enterpriseName", enterpriseName);
        try {
            return rest.post()
                    .uri("/invoice/title/query")
                    .header("Authorization", "APPCODE " + appcode)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JumeiResponse.class);
        } catch (RestClientException e) {
            // 网关 4xx/5xx(配额烧完/appcode 失效)与网络故障都归这里:不缓存,原因透给前端提示
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw InvoiceException.registryUnavailable(reason.length() > 120 ? reason.substring(0, 120) : reason);
        }
    }

    private CacheEntry freshEntry(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        Duration ttl = entry.record() == null ? NEGATIVE_TTL : POSITIVE_TTL;
        if (entry.at().plus(ttl).isBefore(Instant.now())) {
            cache.remove(key);
            return null;
        }
        return entry;
    }

    /// 数据体 → 领域记录:全字段 strip、空归 null,address 尾部电话拆进 phone。
    static CompanyRecord toRecord(JumeiData data) {
        String[] addressPhone = splitAddressPhone(clean(data.address()));
        return new CompanyRecord(clean(data.enterpriseName()), clean(data.taxNo()),
                addressPhone[0], addressPhone[1], clean(data.bankName()), clean(data.bankNo()));
    }

    /// [地址, 电话]:尾部拆不出电话(或拆完地址为空)则原样归地址、电话为 null。
    static String[] splitAddressPhone(String raw) {
        if (raw == null) {
            return new String[] {null, null};
        }
        Matcher m = TRAILING_PHONE.matcher(raw);
        if (m.find()) {
            String address = raw.substring(0, m.start()).strip();
            if (!address.isEmpty()) {
                return new String[] {address, m.group(1).strip()};
            }
        }
        return new String[] {raw, null};
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
