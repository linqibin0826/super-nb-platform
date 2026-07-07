package me.supernb.sub2api;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

/// sub2api 防腐层装配:introspect 客户端 + 独立只读 JdbcTemplate + 充值读模型。
@Configuration
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiConfig {

    @Bean
    public Sub2apiIntrospectClient sub2apiIntrospectClient(Sub2apiProperties props) {
        RestClient restClient = RestClient.builder().baseUrl(props.getBaseUrl()).build();
        return new Sub2apiIntrospectClient(restClient, props.getIntrospectCacheSeconds());
    }

    /// 独立只读 DataSource 包成 JdbcTemplate;与主 DataSource(snb 库)隔离,只碰 sub2api 库。
    @Bean
    @Qualifier("sub2apiReadJdbc")
    public JdbcTemplate sub2apiReadJdbc(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = DataSourceBuilder.create()
                .url(rd.getUrl())
                .username(rd.getUsername())
                .password(rd.getPassword())
                .build();
        return new JdbcTemplate(ds);
    }

    @Bean
    public RechargeReadModel rechargeReadModel(@Qualifier("sub2apiReadJdbc") JdbcTemplate sub2apiReadJdbc) {
        return new JdbcRechargeReadModel(sub2apiReadJdbc);
    }
}
