package me.supernb.gallery.adapter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/// 每 IP 令牌桶限流,只作用于 /gallery/ 路径(不碰 activity / 付费 API)。超限 429 + Retry-After。
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final TokenBucket bucket;

    public RateLimitFilter(
            @Value("${gallery.ratelimit.burst:60}") double burst,
            @Value("${gallery.ratelimit.per-minute:120}") double perMinute) {
        this.bucket = new TokenBucket(burst, perMinute);
    }

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
