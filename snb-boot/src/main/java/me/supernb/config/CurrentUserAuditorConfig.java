package me.supernb.config;

import java.util.Optional;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/// JPA 审计操作人装配:把 `@CurrentUser` 解析出的登录用户接给 `@CreatedBy` / `@LastModifiedBy`,
/// 覆盖 commons-starter-jpa 的默认空实现。
///
/// 是 snb-sub2api 鉴权结果与 JPA 审计基座之间唯一的交汇点,只能住在两者都可见的
/// 组装点(composition root),不属于任何一个限界上下文。**Bean 名必须是 `auditorAware`**——
/// commons-starter-jpa 的 `@EnableJpaAuditing` 用 `auditorAwareRef = "auditorAware"`
/// 按名字取这个 Bean,改名会让容器直接起不来,不是「装配了但没生效」这种能事后
/// 从行为上发现的软失败。
@Configuration(proxyBeanMethods = false)
public class CurrentUserAuditorConfig {

    /// 审计操作人提供者:已鉴权用户画像挂在当前请求属性时给出其 id;
    /// 无请求上下文(定时任务/迁移)或匿名请求一律返回 empty,审计列留 NULL。
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
