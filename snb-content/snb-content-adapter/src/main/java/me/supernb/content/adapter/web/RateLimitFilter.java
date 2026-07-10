package me.supernb.content.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// 每 IP 令牌桶限流，只作用于 `/content/v1/` 公开路径（admin 前缀豁免——它有 token 门且发布管线
/// 批量 upsert 不该被节流），绝不把限流套到其他上下文或付费模型 API 路径上。超限 429 + Retry-After。
///
/// bean 名显式带上下文前缀：gallery 有同类名 filter，默认名 `rateLimitFilter` 在 boot 全栈上下文会撞车。
@Component("contentRateLimitFilter")
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucket bucket;

    /// 构造：接收令牌桶容量与每分钟补充速率（`content.ratelimit.*` 未配置时默认 burst=60、
    /// perMinute=120），据此建一个 TokenBucket 实例。
    public RateLimitFilter(
            @Value("${content.ratelimit.burst:60}") double burst,
            @Value("${content.ratelimit.per-minute:120}") double perMinute) {
        this.bucket = new TokenBucket(burst, perMinute);
    }

    /// 非 `/content/v1/` 路径与 admin 前缀直接放行；命中的路径按 IP（取 X-Forwarded-For 最后一跳）
    /// 过令牌桶，不足则 429 + Retry-After，不再继续走过滤器链。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/content/v1/") || path.startsWith("/content/v1/admin/")) {
            chain.doFilter(request, response);
            return;
        }
        String key = TokenBucket.clientKey(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
        if (!bucket.allow(key)) {
            response.setStatus(429); // Too Many Requests（servlet 无此常量）
            response.setHeader("Retry-After", "10");
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"too many requests\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
