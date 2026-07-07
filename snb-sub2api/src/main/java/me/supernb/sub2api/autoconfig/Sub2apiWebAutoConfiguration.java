package me.supernb.sub2api.autoconfig;

import java.util.List;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/// web 集成装配:把 `@CurrentUser` 参数解析器注册进 MVC——仅 servlet web 环境且 webmvc 在 classpath 时生效。
/// 消费体验压缩到极致:新上下文的 controller 方法只需声明 `@CurrentUser UserProfile user` 参数,零鉴权样板代码。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnBean(Sub2apiIntrospectClient.class)
public class Sub2apiWebAutoConfiguration {

    /// 组装挂了 [CurrentUserArgumentResolver] 的 WebMvcConfigurer,注入 MVC 参数解析链。
    @Bean
    public WebMvcConfigurer sub2apiCurrentUserResolverConfigurer(Sub2apiIntrospectClient introspect) {
        return new WebMvcConfigurer() {
            /// 把 [CurrentUserArgumentResolver] 加入解析器链。
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserArgumentResolver(introspect));
            }
        };
    }
}
