package me.supernb.invoice.adapter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/// 每 IP 令牌桶限流，覆盖 `/invoice/v1/` 全路径（含 admin 前缀——发票 admin 是浏览器人工操作、
/// 无批量管线，不像 content 那样值得豁免；顺带补了活动系统安全审计⑤「读端点零限流」在本上下文的坑），
/// 绝不把限流套到其他上下文或付费模型 API 路径上。超限 429 + Retry-After。
/// XFF 取最后一跳=审计④取证口径（首值可伪造投毒）。
///
/// bean 名显式带上下文前缀：content/gallery 有同类名 filter，默认名 `rateLimitFilter` 在 boot 全栈上下文会撞车。
@Component("invoiceRateLimitFilter")
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucket bucket;

    /// 构造：接收令牌桶容量与每分钟补充速率（`invoice.ratelimit.*` 未配置时默认 burst=30、
    /// perMinute=60），据此建一个 TokenBucket 实例。
    public RateLimitFilter(
            @Value("${invoice.ratelimit.burst:30}") double burst,
            @Value("${invoice.ratelimit.per-minute:60}") double perMinute) {
        this.bucket = new TokenBucket(burst, perMinute);
    }

    /// 非 `/invoice/v1/` 路径直接放行（admin 前缀不豁免）；命中的路径按 IP（取 X-Forwarded-For
    /// 最后一跳）过令牌桶，不足则 429 + Retry-After，不再继续走过滤器链。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/invoice/v1/")) {
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
