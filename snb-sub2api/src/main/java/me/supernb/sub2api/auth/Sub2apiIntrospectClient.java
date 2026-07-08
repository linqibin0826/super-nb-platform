package me.supernb.sub2api.auth;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.RestClient;

/// sub2api 鉴权防腐层:把浏览器带来的 Authorization 头转发给 sub2api 验签,换回用户身份。
///
/// 唯一可信来源是校验过的 token——cookie 里的 `user.id` 客户端可伪造,绝不采信。
/// 转发 `GET {base}/api/v1/user/profile`,响应体按 `{data:{id,role,status}}` 或顶层平铺两种形状容错解析。
/// 进程内短缓存(默认 30 秒)吸收同一 token 的高频复验;网络失败、解析失败、非 2xx 响应一律降级为空结果。
public class Sub2apiIntrospectClient {

    /// 进程内缓存的默认条目上限:key 是完整 token,refresh 一次性轮换会不断产出新 token,
    /// 不设界则过期条目只增不减、内存单调泄漏。达界即触发清理(见 [#evictIfNeeded])。
    private static final int DEFAULT_MAX_ENTRIES = 10_000;

    private final RestClient restClient;
    private final long cacheMillis;
    private final int maxEntries;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    /// 构造:注入转发用的 RestClient 与进程内缓存 TTL(秒),条目上限取默认值。
    public Sub2apiIntrospectClient(RestClient restClient, long cacheSeconds) {
        this(restClient, cacheSeconds, DEFAULT_MAX_ENTRIES);
    }

    /// 构造(可注入条目上限):cacheSeconds 为缓存 TTL 秒数,maxEntries 为进程内缓存条目上限。
    /// 上限入参便于测试用小容量断言收敛,生产走上面的默认上限。
    Sub2apiIntrospectClient(RestClient restClient, long cacheSeconds, int maxEntries) {
        this.restClient = restClient;
        this.cacheMillis = Duration.ofSeconds(cacheSeconds).toMillis();
        this.maxEntries = maxEntries;
    }

    /// 转发 Authorization 头验证身份,authorizationHeader 形如 `"Bearer xxx"`;
    /// 空值直接返回 empty,不经过缓存;无效凭证/请求失败得到的 empty 结果会按类文档所述 TTL 写入负缓存。
    public Optional<UserProfile> introspect(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Optional.empty();
        }
        Cached hit = cache.get(authorizationHeader);
        long now = System.currentTimeMillis();
        if (hit != null && now < hit.expiresAt) {
            return Optional.ofNullable(hit.profile);
        }
        Optional<UserProfile> resolved = fetch(authorizationHeader);
        evictIfNeeded(now);
        cache.put(authorizationHeader, new Cached(resolved.orElse(null), now + cacheMillis));
        return resolved;
    }

    /// 写入前的有界维护:未达上限直接返回;达上限先清掉所有已过期条目,
    /// 若清理后仍达上限(活跃 token 真的超配)则整表作废——退化为无缓存直连 sub2api,
    /// 功能不受影响、内存必然有界。并发下多线程可能重复清理,removeIf/clear 幂等无害。
    private void evictIfNeeded(long now) {
        if (cache.size() < maxEntries) {
            return;
        }
        cache.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
        if (cache.size() >= maxEntries) {
            cache.clear();
        }
    }

    /// 真正发起 introspect HTTP 调用,由 `introspect` 在缓存未命中时触发。
    /// 响应体按 `{data:{...}}` 或顶层平铺两种形状容错解析;缺少数值型 `id` 字段或调用抛出任何异常都返回 empty,
    /// 不区分具体失败原因,是否转 401 交由调用方判断。
    private Optional<UserProfile> fetch(String authorizationHeader) {
        try {
            // 落 Map 而非具体类型:不绑定 Jackson 版本,未知字段被自然忽略,不会因响应多余字段解析失败
            Map<?, ?> body = restClient.get()
                    .uri("/api/v1/user/profile")
                    .header("Authorization", authorizationHeader)
                    .retrieve()
                    .body(Map.class);
            if (body == null) {
                return Optional.empty();
            }
            Map<?, ?> u = (body.get("data") instanceof Map<?, ?> data) ? data : body;
            if (!(u.get("id") instanceof Number id)) {
                return Optional.empty();
            }
            return Optional.of(new UserProfile(id.longValue(), asString(u.get("role")), asString(u.get("status"))));
        } catch (Exception e) {
            // 401/超时/解析失败统一降级为「无身份」,是否对外报 401 交由调用方决定
            return Optional.empty();
        }
    }

    /// JSON 值安全转字符串:null 输入返回 null,其余调用 toString()。
    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /// 当前缓存条目数,仅供测试断言缓存收敛(不随不同 token 数无限增长)。
    int cacheSize() {
        return cache.size();
    }

    /// introspect 结果的缓存条目,以 Authorization 头为 key。
    ///
    /// @param profile   introspect 结果;失败也一并缓存,此时为 null(负缓存,TTL 内不重试)
    /// @param expiresAt 过期时间戳,与 `System.currentTimeMillis()` 同口径
    private record Cached(UserProfile profile, long expiresAt) {
    }
}
