package me.supernb.activity.adapter.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/// `/activity/v1/raffle/*` 公共限流:按客户端 IP 令牌桶,防接口被脚本硬刷
/// (raffle 查询实时打库,无缓存层兜底;站长 2026-07-12 要求)。
///
/// 参数:桶容量 40、每秒回填 10——真人页面开奖期也就 ~0.2 req/s,同一出口 NAT
/// 下几十人共用也远碰不到线;脚本连打立刻 429。
/// 桶表零依赖有界(照 introspect 缓存有界化先例 a14b6e0):超上限先清闲置桶,
/// 仍超则整表重置——宁可对攻击者短暂放行,不可让内存无界增长。
/// IP 取 X-Forwarded-For **最后一个值**:Caddy(trusted_proxies 未配)会丢弃来路
/// XFF 重写为真实对端,最后一值恒为 Caddy 亲验的 IP;取首值会被伪造头绕过。
@Component
public class RaffleRateLimitFilter extends OncePerRequestFilter {

    static final int CAPACITY = 40;
    static final double REFILL_PER_SEC = 10.0;
    private static final int MAX_BUCKETS = 20_000;
    private static final long IDLE_EVICT_NANOS = 60L * 1_000_000_000L;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /// 单 IP 令牌桶:按流逝时间线性回填,封顶容量。
    static final class Bucket {
        private double tokens = CAPACITY;
        private long last = System.nanoTime();

        synchronized boolean tryTake() {
            long now = System.nanoTime();
            tokens = Math.min(CAPACITY, tokens + (now - last) / 1e9 * REFILL_PER_SEC);
            last = now;
            if (tokens < 1) {
                return false;
            }
            tokens -= 1;
            return true;
        }

        synchronized long idleNanos(long now) {
            return now - last;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/activity/v1/raffle");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Bucket bucket = buckets.computeIfAbsent(clientIp(request), k -> {
            evictIfOversize();
            return new Bucket();
        });
        if (bucket.tryTake()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"请求太频繁,稍后再试\"}");
    }

    private void evictIfOversize() {
        if (buckets.size() < MAX_BUCKETS) {
            return;
        }
        long now = System.nanoTime();
        buckets.entrySet().removeIf(e -> e.getValue().idleNanos(now) > IDLE_EVICT_NANOS);
        if (buckets.size() >= MAX_BUCKETS) {
            buckets.clear();
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
