package me.supernb.content.adapter.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/// content 限流纯单测：只限公开 content 路径，admin 与他上下文路径豁免。
class RateLimitFilterTest {

    MockHttpServletRequest req(String uri) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setRequestURI(uri);
        r.setRemoteAddr("1.2.3.4");
        return r;
    }

    @Test
    void publicContentPathIsThrottled() throws Exception {
        var filter = new RateLimitFilter(2, 0.0001); // burst=2，补充速率趋零
        FilterChain chain = mock(FilterChain.class);

        var r1 = new MockHttpServletResponse();
        var r2 = new MockHttpServletResponse();
        var r3 = new MockHttpServletResponse();
        filter.doFilter(req("/content/v1/articles"), r1, chain);
        filter.doFilter(req("/content/v1/articles"), r2, chain);
        filter.doFilter(req("/content/v1/articles"), r3, chain);

        assertThat(r1.getStatus()).isEqualTo(200);
        assertThat(r2.getStatus()).isEqualTo(200);
        assertThat(r3.getStatus()).isEqualTo(429);
        assertThat(r3.getHeader("Retry-After")).isEqualTo("10");
    }

    @Test
    void adminAndForeignPathsAreExempt() throws Exception {
        var filter = new RateLimitFilter(1, 0.0001);
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 3; i++) {
            var res = new MockHttpServletResponse();
            filter.doFilter(req("/content/v1/admin/articles:upsert"), res, chain);
            assertThat(res.getStatus()).isEqualTo(200); // admin 豁免（token 门另管）
        }
        var foreign = new MockHttpServletResponse();
        filter.doFilter(req("/gallery/v1/prompts"), foreign, chain);
        assertThat(foreign.getStatus()).isEqualTo(200); // 他上下文不归本 filter 管
    }
}
