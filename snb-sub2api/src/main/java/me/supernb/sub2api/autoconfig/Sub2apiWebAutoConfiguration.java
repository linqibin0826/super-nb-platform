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

/// web 集成装配:把 @CurrentUser 参数解析器注入 MVC(仅 servlet web 环境 + webmvc 在 classpath 时生效)。
/// 新上下文的 controller 只需声明 `@CurrentUser UserProfile user`,零鉴权样板。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnBean(Sub2apiIntrospectClient.class)
public class Sub2apiWebAutoConfiguration {

    /// 向 MVC 注册 @CurrentUser 参数解析器。
    @Bean
    public WebMvcConfigurer sub2apiCurrentUserResolverConfigurer(Sub2apiIntrospectClient introspect) {
        return new WebMvcConfigurer() {
            /// 挂载 CurrentUserArgumentResolver。
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserArgumentResolver(introspect));
            }
        };
    }
}
