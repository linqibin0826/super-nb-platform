package me.supernb.sub2api.autoconfig;

import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/// sub2api 防腐层的基础装配:introspect 鉴权客户端,所有上下文共用、随 classpath 自动生效。
/// 经 `AutoConfiguration.imports` 自注册,不依赖宿主的组件扫描。
@AutoConfiguration
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiAutoConfiguration {

    /// introspect 客户端 Bean:常开能力,宿主声明同类型 Bean 即可覆盖。
    @Bean
    @ConditionalOnMissingBean
    public Sub2apiIntrospectClient sub2apiIntrospectClient(Sub2apiProperties props) {
        RestClient restClient = RestClient.builder().baseUrl(props.getBaseUrl()).build();
        return new Sub2apiIntrospectClient(restClient, props.getIntrospectCacheSeconds());
    }
}
