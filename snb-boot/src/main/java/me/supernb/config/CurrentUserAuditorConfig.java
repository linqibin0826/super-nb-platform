package me.supernb.config;

import java.util.Optional;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/// JPA 审计操作人装配(组合根横切):把 `@CurrentUser` 解析出的登录用户
/// 接给 `@CreatedBy` / `@LastModifiedBy`。
///
/// 覆盖 commons-starter-jpa 的默认空实现;**Bean 名必须是 `auditorAware`**
/// (starter 的 `@EnableJpaAuditing(auditorAwareRef = "auditorAware")` 按名引用)。
@Configuration(proxyBeanMethods = false)
public class CurrentUserAuditorConfig {

    /// 审计操作人提供者:读当前请求属性里的已鉴权用户画像;
    /// 无请求上下文(定时任务/迁移)或匿名请求返回 empty(审计列留 NULL)。
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return Optional.empty();
            }
            Object profile = attrs.getAttribute(
                    CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
            return profile instanceof UserProfile p ? Optional.of(p.id()) : Optional.empty();
        };
    }
}
