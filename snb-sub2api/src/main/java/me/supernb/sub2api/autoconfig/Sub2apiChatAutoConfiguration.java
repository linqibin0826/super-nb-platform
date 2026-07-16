package me.supernb.sub2api.autoconfig;

import java.net.http.HttpClient;
import java.time.Duration;
import me.supernb.sub2api.chat.Sub2apiChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/// sub2api chat 通道装配:不配 `sub2api.invoice-ai-key` 就完全不装配(照 admin-key 惯例,
/// yml 绝不给默认值,env `SUB2API_INVOICE_AI_KEY` 缺席即 fail-closed)。超时收紧但读窗留足
/// (LLM 首字节慢,实测 luna 单次 ~5s)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api", name = "invoice-ai-key")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiChatAutoConfiguration {

    /// chat 客户端 Bean(Bearer 默认头 + 连 5s/读 25s)。
    @Bean
    @ConditionalOnMissingBean
    public Sub2apiChatClient sub2apiChatClient(Sub2apiProperties props) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(25));
        RestClient restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl() + "/v1")
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + props.getInvoiceAiKey())
                .build();
        return new Sub2apiChatClient(restClient, props.getInvoiceAiModel());
    }
}
