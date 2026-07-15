package me.supernb.invoice.adapter.web;

import java.util.concurrent.ConcurrentHashMap;

/// 每 key 令牌桶，逐字节拷自 gallery 同名类（源头是 gallery-svc 的 `ratelimit.py`）。
/// 容量为 burst，以恒定速率 perMinute/分钟补充令牌。
///
/// 阈值按"人类正常滚动够不到、脚本狂拉全库会被拒"来定；状态存进程内内存，单容器部署下足够。
public class TokenBucket {

    private static final int PRUNE_THRESHOLD = 10_000;
    private static final double IDLE_TTL_SEC = 600.0;

    private final double capacity;
    private final double refillPerSecond;
    // key -> [剩余令牌, 上次结算秒]
    private final ConcurrentHashMap<String, double[]> buckets = new ConcurrentHashMap<>();

    /// 构造：接收 burst（桶容量）与 perMinute（每分钟补充令牌数），内部换算成 refillPerSecond 恒定速率。
    public TokenBucket(double burst, double perMinute) {
        this.capacity = burst;
        this.refillPerSecond = perMinute / 60.0;
    }

    /// 消费一枚令牌，不够则拒绝；线程安全，用当前系统时刻作为结算时间。
    public boolean allow(String key) {
        return allow(key, System.nanoTime() / 1_000_000_000.0);
    }

    /// 惰性补令牌：令牌数按 nowSec 与上次结算时刻的差值补齐，封顶 capacity；够 1 枚则扣除后放行，
    /// 不够则把刚补出的（仍不足 1 的）令牌数连同这次结算时刻写回再拒绝，状态不是原地不动。
    /// `synchronized` 保证并发安全；显式收时刻参数是为了让测试摆脱系统时钟，注入任意时间点验证补充逻辑。
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

    /// 清掉超过 IDLE_TTL_SEC 没再结算过的 key（即长期不活跃的桶），防 `buckets` 无限增长占内存。
    private void prune(double nowSec) {
        buckets.entrySet().removeIf(e -> nowSec - e.getValue()[1] > IDLE_TTL_SEC);
    }

    /// 按 X-Forwarded-For 取最后一跳作为客户端标识（生产只有一层 Caddy 反代，故最后一跳即真实来源 IP）；
    /// 头缺失、为空白，或最后一跳本身是空白，都退回 fallback。
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
