package me.supernb.activity.adapter.rest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/// `/activity/v1/checkin*` 独立限流:按客户端 IP 令牌桶,不与 raffle/gate 共用阈值
/// (spec §6)。容量/回填经 `activity.checkin.rate-limit.*` 可配(env 见 spec §7.5)——
/// 与 RaffleRateLimitFilter 硬编码常量不同,本桶按需要独立调参而非写死在代码里。
/// evict 逻辑必须在 computeIfAbsent 回调之外执行:在其映射函数内对同一 ConcurrentHashMap
/// 做 clear()/removeIf() 违反 JDK 明令,生产同版本 JDK 25 实测高并发下会真死锁、不自愈
/// (runbook ai-relay deployment/31,照 RaffleRateLimitFilter 修复实现抄写)。
/// IP 取 X-Forwarded-For 最后一个值:Caddy(trusted_proxies 未配)会丢弃来路 XFF 重写为
/// 真实对端,最后一值恒为 Caddy 亲验的 IP;取首值会被伪造头绕过。
@Component
public class CheckinRateLimitFilter extends OncePerRequestFilter {

    static final int DEFAULT_MAX_BUCKETS = 20_000;
    private static final long IDLE_EVICT_NANOS = 60L * 1_000_000_000L;

    private final int capacity;
    private final double refillPerSec;
    private final int maxBuckets;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /// 构造(Spring 用):容量/回填经配置注入(默认 30 枚桶、5/s 回填),桶表上限取默认 20000。
    /// 双构造器场景必须 @Autowired 消歧,否则容器回退找无参构造器失败、应用上下文刷新崩溃。
    @Autowired
    public CheckinRateLimitFilter(
            @Value("${activity.checkin.rate-limit.capacity:30}") int capacity,
            @Value("${activity.checkin.rate-limit.refill-per-sec:5}") double refillPerSec) {
        this(capacity, refillPerSec, DEFAULT_MAX_BUCKETS);
    }

    /// 构造(可注入桶表上限):便于测试用小容量断言溢出淘汰路径收敛(照 introspect 缓存有界化先例)。
    CheckinRateLimitFilter(int capacity, double refillPerSec, int maxBuckets) {
        this.capacity = capacity;
        this.refillPerSec = refillPerSec;
        this.maxBuckets = maxBuckets;
    }

    /// 单 IP 令牌桶:按流逝时间线性回填,封顶容量(容量/回填取外层实例的可配字段)。
    final class Bucket {
        private double tokens = capacity;
        private long last = System.nanoTime();

        synchronized boolean tryTake() {
            long now = System.nanoTime();
            tokens = Math.min(capacity, tokens + (now - last) / 1e9 * refillPerSec);
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
        return !request.getRequestURI().startsWith("/activity/v1/checkin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        evictIfOversize();
        Bucket bucket = buckets.computeIfAbsent(clientIp(request), k -> new Bucket());
        if (bucket.tryTake()) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"请求太频繁,稍后再试\"}");
    }

    private void evictIfOversize() {
        if (buckets.size() < maxBuckets) {
            return;
        }
        long now = System.nanoTime();
        buckets.entrySet().removeIf(e -> e.getValue().idleNanos(now) > IDLE_EVICT_NANOS);
        if (buckets.size() >= maxBuckets) {
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
