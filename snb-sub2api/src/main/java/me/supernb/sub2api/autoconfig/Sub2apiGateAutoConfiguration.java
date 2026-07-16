package me.supernb.sub2api.autoconfig;

import javax.sql.DataSource;
import me.supernb.sub2api.gate.GateReadModel;
import me.supernb.sub2api.gate.JdbcGateReadModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/// 金票门槛只读读模型装配,按能力条件生效:不配 `sub2api.read-datasource.url` 就完全不装配。
/// 只读 DataSource 在 `@Bean` 方法内部现场构建、绝不对外暴露为 Bean(防挤退 Boot 主 jdbcTemplate,
/// 缘由见 [Sub2apiRechargeAutoConfiguration] 类注释)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiGateAutoConfiguration {

    /// 金票门槛读模型 Bean。
    @Bean
    @ConditionalOnMissingBean
    public GateReadModel gateReadModel(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = rd.build();
        return new JdbcGateReadModel(new JdbcTemplate(ds));
    }
}
