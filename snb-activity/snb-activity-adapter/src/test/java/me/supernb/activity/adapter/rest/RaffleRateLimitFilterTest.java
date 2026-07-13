package me.supernb.activity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/// raffle 限流:突发额度内放行、超限 429、随时间回填、非 raffle 路径不设防、
/// 限流键取 XFF 最后一值(首值可伪造)。
class RaffleRateLimitFilterTest {

    private static MockHttpServletRequest req(String uri, String xff) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setRequestURI(uri);
        if (xff != null) {
            r.addHeader("X-Forwarded-For", xff);
        }
        return r;
    }

    private static int fire(RaffleRateLimitFilter f, String uri, String xff) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req(uri, xff), res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void allowsBurstThenRejectsWithLimitAndRefills() throws Exception {
        RaffleRateLimitFilter f = new RaffleRateLimitFilter();
        for (int i = 0; i < RaffleRateLimitFilter.CAPACITY; i++) {
            assertThat(fire(f, "/activity/v1/raffle/current", "1.2.3.4")).isEqualTo(200);
        }
        assertThat(fire(f, "/activity/v1/raffle/current", "1.2.3.4")).isEqualTo(429);
        Thread.sleep(320); // 10/s 回填,320ms ≥ 3 枚
        assertThat(fire(f, "/activity/v1/raffle/current", "1.2.3.4")).isEqualTo(200);
    }

    @Test
    void perIpIsolationAndXffLastValueWins() throws Exception {
        RaffleRateLimitFilter f = new RaffleRateLimitFilter();
        for (int i = 0; i < RaffleRateLimitFilter.CAPACITY; i++) {
            fire(f, "/activity/v1/raffle/current", "9.9.9.9");
        }
        assertThat(fire(f, "/activity/v1/raffle/current", "9.9.9.9")).isEqualTo(429);
        // 伪造首值、真实对端(最后一值)相同 → 仍被同桶拦住,伪造无效
        assertThat(fire(f, "/activity/v1/raffle/current", "8.8.8.8, 9.9.9.9")).isEqualTo(429);
        // 真实对端不同 → 各自成桶,不受影响
        assertThat(fire(f, "/activity/v1/raffle/current", "5.6.7.8")).isEqualTo(200);
    }

    @Test
    void otherPathsBypassFilter() throws Exception {
        RaffleRateLimitFilter f = new RaffleRateLimitFilter();
        for (int i = 0; i < RaffleRateLimitFilter.CAPACITY + 5; i++) {
            assertThat(fire(f, "/activity/v1/usage-leaderboard", "1.2.3.4")).isEqualTo(200);
        }
    }

    /// evict 挪出 computeIfAbsent 回调后:并发逼近桶表上限不再触发 ConcurrentHashMap 嵌套自变更死锁
    /// (旧实现在生产同版本 JDK 25 实测 16 线程构成 ReservationNode 循环等待、永不返回)。此测试在
    /// 修复前会超时,修复后秒级完成。
    @Test
    void tableOverflowUnderConcurrencyDoesNotDeadlock() {
        RaffleRateLimitFilter f = new RaffleRateLimitFilter(64);
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            int threads = 16;
            Thread[] ts = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                final int base = t;
                ts[t] = new Thread(() -> {
                    for (int i = 0; i < 2000; i++) {
                        try {
                            // 每线程各刷不同 IP 段,持续把桶表顶到上限、反复触发 evict
                            fire(f, "/activity/v1/raffle/current",
                                    "10." + base + "." + (i / 256 % 256) + "." + (i % 256));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
            for (Thread th : ts) {
                th.start();
            }
            for (Thread th : ts) {
                th.join();
            }
        });
    }
}
