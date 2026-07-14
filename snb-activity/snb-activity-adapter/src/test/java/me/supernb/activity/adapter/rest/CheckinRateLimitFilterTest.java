package me.supernb.activity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/// 签到限流:突发额度内放行、超限 429、随时间回填、非 checkin 路径不设防、
/// 限流键取 XFF 最后一值、桶表溢出淘汰不死锁——照 RaffleRateLimitFilterTest 断言口径,
/// 独立建桶不与 raffle/gate 共用阈值(spec §6)。
class CheckinRateLimitFilterTest {

    private static MockHttpServletRequest req(String uri, String xff) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", uri);
        r.setRequestURI(uri);
        if (xff != null) {
            r.addHeader("X-Forwarded-For", xff);
        }
        return r;
    }

    private static int fire(CheckinRateLimitFilter f, String uri, String xff) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req(uri, xff), res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void allowsBurstThenRejectsWithLimitAndRefills() throws Exception {
        CheckinRateLimitFilter f = new CheckinRateLimitFilter(10, 10.0, CheckinRateLimitFilter.DEFAULT_MAX_BUCKETS);
        for (int i = 0; i < 10; i++) {
            assertThat(fire(f, "/activity/v1/checkin", "1.2.3.4")).isEqualTo(200);
        }
        assertThat(fire(f, "/activity/v1/checkin", "1.2.3.4")).isEqualTo(429);
        Thread.sleep(320); // 10/s 回填,320ms ≥ 3 枚
        assertThat(fire(f, "/activity/v1/checkin", "1.2.3.4")).isEqualTo(200);
    }

    @Test
    void perIpIsolationAndXffLastValueWins() throws Exception {
        CheckinRateLimitFilter f = new CheckinRateLimitFilter(5, 10.0, CheckinRateLimitFilter.DEFAULT_MAX_BUCKETS);
        for (int i = 0; i < 5; i++) {
            fire(f, "/activity/v1/checkin/status", "9.9.9.9");
        }
        assertThat(fire(f, "/activity/v1/checkin/status", "9.9.9.9")).isEqualTo(429);
        // 伪造首值、真实对端(最后一值)相同 → 仍被同桶拦住
        assertThat(fire(f, "/activity/v1/checkin/status", "8.8.8.8, 9.9.9.9")).isEqualTo(429);
        assertThat(fire(f, "/activity/v1/checkin/status", "5.6.7.8")).isEqualTo(200);
    }

    @Test
    void otherPathsBypassFilter() throws Exception {
        CheckinRateLimitFilter f = new CheckinRateLimitFilter(2, 1.0, CheckinRateLimitFilter.DEFAULT_MAX_BUCKETS);
        for (int i = 0; i < 10; i++) {
            assertThat(fire(f, "/activity/v1/raffle/current", "1.2.3.4")).isEqualTo(200);
        }
    }

    /// evict 挪出 computeIfAbsent 回调后:并发逼近桶表上限不再触发 ConcurrentHashMap 嵌套自变更死锁
    /// (runbook ai-relay deployment/31 实证教训,照 RaffleRateLimitFilter 修复实现)。
    @Test
    void tableOverflowUnderConcurrencyDoesNotDeadlock() {
        CheckinRateLimitFilter f = new CheckinRateLimitFilter(30, 5.0, 64);
        assertTimeoutPreemptively(Duration.ofSeconds(15), () -> {
            int threads = 16;
            Thread[] ts = new Thread[threads];
            for (int t = 0; t < threads; t++) {
                final int base = t;
                ts[t] = new Thread(() -> {
                    for (int i = 0; i < 2000; i++) {
                        try {
                            fire(f, "/activity/v1/checkin",
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
