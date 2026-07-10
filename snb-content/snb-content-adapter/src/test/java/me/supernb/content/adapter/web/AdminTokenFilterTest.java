package me.supernb.content.adapter.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/// admin token 门纯单测：fail-closed 语义 + 路径前缀边界。
class AdminTokenFilterTest {

    MockHttpServletRequest req(String uri, String token) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", uri);
        r.setRequestURI(uri);
        if (token != null) {
            r.addHeader("X-Admin-Token", token);
        }
        return r;
    }

    @Test
    void nonAdminPathPassesThroughUntouched() throws Exception {
        var filter = new AdminTokenFilter("secret");
        var res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req("/content/v1/articles", null), res, chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void missingTokenGets401() throws Exception {
        var filter = new AdminTokenFilter("secret");
        var res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req("/content/v1/admin/articles:upsert", null), res, chain);

        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void wrongTokenGets401() throws Exception {
        var filter = new AdminTokenFilter("secret");
        var res = new MockHttpServletResponse();

        filter.doFilter(req("/content/v1/admin/articles:upsert", "nope"), res, mock(FilterChain.class));

        assertThat(res.getStatus()).isEqualTo(401);
    }

    @Test
    void correctTokenPasses() throws Exception {
        var filter = new AdminTokenFilter("secret");
        var res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req("/content/v1/admin/articles:upsert", "secret"), res, chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void blankConfiguredTokenFailsClosed() throws Exception {
        var filter = new AdminTokenFilter("");
        var res = new MockHttpServletResponse();

        filter.doFilter(req("/content/v1/admin/articles:upsert", "anything"), res, mock(FilterChain.class));

        assertThat(res.getStatus()).isEqualTo(401); // env 缺失时宁可全拒，不可裸奔
    }
}
