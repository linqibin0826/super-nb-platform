package me.supernb.sub2api.autoconfig;

import javax.sql.DataSource;
import me.supernb.sub2api.account.AccountAgeReadModel;
import me.supernb.sub2api.account.JdbcAccountAgeReadModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/// 账号年龄只读读模型装配:不配 `sub2api.read-datasource.url` 就完全不装配
/// (照 gate/raffle/recharge/referral/usage-board 家族条件装配惯例)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiAccountAutoConfiguration {

    /// 账号年龄读模型 Bean:只读 DataSource 在本方法内现场构建,不对外暴露。
    @Bean
    @ConditionalOnMissingBean
    public AccountAgeReadModel accountAgeReadModel(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = rd.build();
        return new JdbcAccountAgeReadModel(new JdbcTemplate(ds));
    }
}
