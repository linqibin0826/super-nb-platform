package me.supernb.activity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/// school 前缀限流:非 school 路径不过滤、桶耗尽 429、XFF 取末值。
/// 「桶保持空 → 429」断言的 refill 用 0.05/s——慢 runner 上 refill 快于断言窗口
/// 会假失败(runbook ai-relay 32 的 CI 坑,凡此类断言恒用 0.05/s 级)。
class SchoolRateLimitFilterTest {

    private static MockHttpServletRequest req(String uri, String xff) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        if (xff != null) {
            request.addHeader("X-Forwarded-For", xff);
        }
        return request;
    }

    @Test
    void nonSchoolPathNotFiltered() {
        SchoolRateLimitFilter filter = new SchoolRateLimitFilter(2, 0.05, 100);
        assertThat(filter.shouldNotFilter(req("/activity/v1/checkin/status", null))).isTrue();
        assertThat(filter.shouldNotFilter(req("/activity/v1/school/status", null))).isFalse();
        assertThat(filter.shouldNotFilter(req("/activity/v1/school/leaderboard", null))).isFalse();
    }

    @Test
    void burstThenEmpty429() throws Exception {
        SchoolRateLimitFilter filter = new SchoolRateLimitFilter(2, 0.05, 100);
        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse ok = new MockHttpServletResponse();
            filter.doFilter(req("/activity/v1/school/status", null), ok, new MockFilterChain());
            assertThat(ok.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse limited = new MockHttpServletResponse();
        filter.doFilter(req("/activity/v1/school/status", null), limited, new MockFilterChain());
        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getContentAsString()).contains("请求太频繁");
    }

    @Test
    void xffTakesLastHop() throws Exception {
        SchoolRateLimitFilter filter = new SchoolRateLimitFilter(1, 0.05, 100);
        // 伪造首值不同、末值相同:应命中同一个桶 → 第二发 429
        MockHttpServletResponse first = new MockHttpServletResponse();
        filter.doFilter(req("/activity/v1/school/status", "6.6.6.6, 2.2.2.2"), first, new MockFilterChain());
        assertThat(first.getStatus()).isEqualTo(200);
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(req("/activity/v1/school/status", "9.9.9.9, 2.2.2.2"), second, new MockFilterChain());
        assertThat(second.getStatus()).isEqualTo(429);

        FilterChain chain = new MockFilterChain();
        // 末值不同 = 不同桶,不受影响
        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(req("/activity/v1/school/status", "9.9.9.9, 3.3.3.3"), other, chain);
        assertThat(other.getStatus()).isEqualTo(200);
    }
}
