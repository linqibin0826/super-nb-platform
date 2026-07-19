package me.supernb.sub2api.autoconfig;

import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import me.supernb.sub2api.admin.Sub2apiAdminRedeemCodeClient;
import me.supernb.sub2api.admin.Sub2apiAdminSubscriptionClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/// sub2api admin 写能力装配:不配 `sub2api.admin-key` 就完全不装配(照 recharge/raffle/gate/
/// usage-board 只读能力家族的条件装配惯例,只是本条件是 admin-key 而非 read-datasource.url)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api", name = "admin-key")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiAdminAutoConfiguration {

    /// admin 订阅批量分配客户端 Bean。
    @Bean
    @ConditionalOnMissingBean
    public Sub2apiAdminSubscriptionClient sub2apiAdminSubscriptionClient(Sub2apiProperties props) {
        RestClient restClient = RestClient.builder().baseUrl(props.getBaseUrl() + "/api/v1/admin").build();
        return new Sub2apiAdminSubscriptionClient(restClient, props.getAdminKey());
    }

    /// admin 余额扣/退客户端 Bean(发票手续费结算)。
    @Bean
    @ConditionalOnMissingBean
    public Sub2apiAdminBalanceClient sub2apiAdminBalanceClient(Sub2apiProperties props) {
        RestClient restClient = RestClient.builder().baseUrl(props.getBaseUrl() + "/api/v1/admin").build();
        return new Sub2apiAdminBalanceClient(restClient, props.getAdminKey());
    }

    /// admin 兑换码批量生成客户端(抽奖奖品用)。
    @Bean
    @ConditionalOnMissingBean
    public Sub2apiAdminRedeemCodeClient sub2apiAdminRedeemCodeClient(Sub2apiProperties props) {
        RestClient restClient = RestClient.builder().baseUrl(props.getBaseUrl() + "/api/v1/admin").build();
        return new Sub2apiAdminRedeemCodeClient(restClient, props.getAdminKey());
    }
}
