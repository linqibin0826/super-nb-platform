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

    private final RestClient restClient;
    private final long cacheMillis;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    /// 构造:注入转发用的 RestClient 与进程内缓存 TTL(秒)。
    public Sub2apiIntrospectClient(RestClient restClient, long cacheSeconds) {
        this.restClient = restClient;
        this.cacheMillis = Duration.ofSeconds(cacheSeconds).toMillis();
    }

    /// 转发 Authorization 头验证身份,authorizationHeader 形如 `"Bearer xxx"`;
    /// 空值/无效凭证/请求失败一律返回 empty,该结果(含失败)按类文档所述 TTL 一并缓存。
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
        cache.put(authorizationHeader, new Cached(resolved.orElse(null), now + cacheMillis));
        return resolved;
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

    /// introspect 结果的缓存条目,以 Authorization 头为 key。
    ///
    /// @param profile   introspect 结果;失败也一并缓存,此时为 null(负缓存,TTL 内不重试)
    /// @param expiresAt 过期时间戳,与 `System.currentTimeMillis()` 同口径
    private record Cached(UserProfile profile, long expiresAt) {
    }
}
