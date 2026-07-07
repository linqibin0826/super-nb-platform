package me.supernb.sub2api.autoconfig;

import javax.sql.DataSource;
import me.supernb.sub2api.recharge.JdbcRechargeReadModel;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/// 充值只读读模型装配:按能力条件生效——不配 `sub2api.read-datasource.url` 就完全不装
/// (不建只读 DataSource,不逼无关上下文/测试伺候它)。
///
/// 只读 DataSource 与 JdbcTemplate 在 @Bean 方法内部构建、不暴露为 bean:
/// 避免多出一个 JdbcOperations 候选把 Boot 的主 jdbcTemplate 自动配置挤退(条件是
/// @ConditionalOnMissingBean(JdbcOperations)),也免去 @Qualifier 纪律。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiRechargeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RechargeReadModel rechargeReadModel(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = DataSourceBuilder.create()
                .url(rd.getUrl())
                .username(rd.getUsername())
                .password(rd.getPassword())
                .build();
        return new JdbcRechargeReadModel(new JdbcTemplate(ds));
    }
}
