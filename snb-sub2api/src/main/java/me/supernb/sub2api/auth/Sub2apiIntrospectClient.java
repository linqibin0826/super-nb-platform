package me.supernb.sub2api.auth;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.client.RestClient;

/// sub2api 鉴权防腐层:把浏览器带来的 Authorization 头转发给 sub2api 验签,拿回用户身份。
///
/// - 唯一可信来源是校验过的 token(cookie 里的 user.id 客户端可伪造,绝不信)。
/// - 转发 `GET {base}/api/v1/user/profile`,响应体 `{data:{id,role,status}}` 或顶层平铺都容错。
/// - 进程内短缓存(默认 30s)吸收同一 token 的高频复验;任何网络/解析/非 2xx 失败回 empty。
public class Sub2apiIntrospectClient {

    private final RestClient restClient;
    private final long cacheMillis;
    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<>();

    /// 构造:注入底层 RestClient 与缓存 TTL(秒)。
    public Sub2apiIntrospectClient(RestClient restClient, long cacheSeconds) {
        this.restClient = restClient;
        this.cacheMillis = Duration.ofSeconds(cacheSeconds).toMillis();
    }

    /// 转发 Authorization 头验身份。authorizationHeader 形如 "Bearer xxx";空/无效/失败回 empty。
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

    private Optional<UserProfile> fetch(String authorizationHeader) {
        try {
            // 用 Map 反序列化:与 Jackson 版本无关,且天然忽略响应里的其余字段(不会因未知字段失败)
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
            // 401 / 超时 / 解析失败一律降级为「无身份」,由调用方按需返回 401
            return Optional.empty();
        }
    }

    /// 任意 JSON 值安全转字符串(null 安全)。
    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private record Cached(UserProfile profile, long expiresAt) {
    }
}
