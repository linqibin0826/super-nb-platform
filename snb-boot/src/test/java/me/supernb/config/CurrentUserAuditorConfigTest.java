package me.supernb.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.data.domain.AuditorAware;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/// 审计操作人装配单测:只有当前请求带已鉴权画像时给出用户 id,
/// 无请求上下文(迁移/任务)或匿名请求一律 empty(审计列留 NULL)。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CurrentUserAuditorConfigTest {

    final AuditorAware<Long> auditor = new CurrentUserAuditorConfig().auditorAware();

    @AfterEach
    void resetContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void returnsUserIdWhenResolvedProfilePresent() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE,
                new UserProfile(42L, "user", "active"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

        assertThat(auditor.getCurrentAuditor()).contains(42L);
    }

    @Test
    void emptyWithoutRequestContext() {
        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }

    @Test
    void emptyWhenRequestAnonymous() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

        assertThat(auditor.getCurrentAuditor()).isEmpty();
    }
}
