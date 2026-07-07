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

/// 充值只读读模型装配,按能力条件生效:不配 `sub2api.read-datasource.url` 就完全不装配——
/// 不建只读 DataSource,不逼无关上下文/测试也要伺候它。
///
/// 只读 DataSource 与 JdbcTemplate 只在 `@Bean` 方法内部现场构建,绝不对外暴露为 Bean:
/// 一旦暴露,会多出一个 JdbcOperations 候选,把 Boot 主 jdbcTemplate 的自动配置
/// (条件正是 `@ConditionalOnMissingBean(JdbcOperations)`)挤退;不暴露还顺带省掉了
/// 到处加 `@Qualifier` 消歧义的负担。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiRechargeAutoConfiguration {

    /// 充值只读读模型 Bean:只读 DataSource 在本方法内现场构建,不对外暴露为 Bean(防挤退 Boot 主 jdbcTemplate,详见类注释)。
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
