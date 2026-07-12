package me.supernb.activity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;

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
}
