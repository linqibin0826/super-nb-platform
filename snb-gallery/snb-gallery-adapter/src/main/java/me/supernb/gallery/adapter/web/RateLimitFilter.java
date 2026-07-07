package me.supernb.gallery.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/// 每 IP 令牌桶限流,只作用于 `/gallery/` 路径前缀,绝不把限流逻辑套到 activity 或付费模型 API 路径上
/// (那是上游 sub2api 的域)。超限返回 429 + `Retry-After` 头。
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucket bucket;

    /// 构造:接收令牌桶容量与每分钟补充速率(`gallery.ratelimit.*` 未配置时默认 burst=60、
    /// perMinute=120),据此建一个 TokenBucket 实例。
    public RateLimitFilter(
            @Value("${gallery.ratelimit.burst:60}") double burst,
            @Value("${gallery.ratelimit.per-minute:120}") double perMinute) {
        this.bucket = new TokenBucket(burst, perMinute);
    }

    /// 非 `/gallery/` 路径直接放行;命中的路径按 IP(取 X-Forwarded-For 最后一跳)过令牌桶,
    /// 不足则 429 + Retry-After,不再继续走过滤器链。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/gallery/")) {
            chain.doFilter(request, response);
            return;
        }
        String key = TokenBucket.clientKey(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
        if (!bucket.allow(key)) {
            response.setStatus(429); // Too Many Requests(servlet 无此常量)
            response.setHeader("Retry-After", "10");
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"too many requests\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
