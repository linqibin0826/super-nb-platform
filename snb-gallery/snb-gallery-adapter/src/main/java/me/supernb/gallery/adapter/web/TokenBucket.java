package me.supernb.gallery.adapter.web;

import java.util.concurrent.ConcurrentHashMap;

/// 每 key 令牌桶(移植 gallery-svc ratelimit.py)。容量 burst,恒速补 perMinute/分钟。
/// 人类滚动够不到,脚本狂拉全库会被拒。进程内内存态,单容器足够。
public class TokenBucket {

    private static final int PRUNE_THRESHOLD = 10_000;
    private static final double IDLE_TTL_SEC = 600.0;

    private final double capacity;
    private final double refillPerSecond;
    // key -> [剩余令牌, 上次结算秒]
    private final ConcurrentHashMap<String, double[]> buckets = new ConcurrentHashMap<>();

    public TokenBucket(double burst, double perMinute) {
        this.capacity = burst;
        this.refillPerSecond = perMinute / 60.0;
    }

    /// 消费一枚令牌;不够则拒绝。线程安全。
    public boolean allow(String key) {
        return allow(key, System.nanoTime() / 1_000_000_000.0);
    }

    /// 可注入时刻的重载(测试用)。
    public synchronized boolean allow(String key, double nowSec) {
        double[] state = buckets.getOrDefault(key, new double[] {capacity, nowSec});
        double tokens = Math.min(capacity, state[0] + (nowSec - state[1]) * refillPerSecond);
        if (tokens < 1.0) {
            buckets.put(key, new double[] {tokens, nowSec});
            return false;
        }
        buckets.put(key, new double[] {tokens - 1.0, nowSec});
        if (buckets.size() > PRUNE_THRESHOLD) {
            prune(nowSec);
        }
        return true;
    }

    private void prune(double nowSec) {
        buckets.entrySet().removeIf(e -> nowSec - e.getValue()[1] > IDLE_TTL_SEC);
    }

    /// X-Forwarded-For 取最后一跳(生产恰一层 Caddy 反代);缺失退回 fallback。
    public static String clientKey(String xffHeader, String fallback) {
        if (xffHeader != null && !xffHeader.isBlank()) {
            String[] hops = xffHeader.split(",");
            String last = hops[hops.length - 1].strip();
            if (!last.isEmpty()) {
                return last;
            }
        }
        return fallback;
    }
}
