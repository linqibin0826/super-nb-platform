package me.supernb.sub2api.autoconfig;

import javax.sql.DataSource;
import me.supernb.sub2api.usageboard.JdbcUsageBoardReadModel;
import me.supernb.sub2api.usageboard.UsageBoardReadModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/// 用量排行榜只读读模型装配:不配 `sub2api.read-datasource.url` 就完全不装配。
/// 只读 DataSource 与 JdbcTemplate 只在 `@Bean` 方法内部现场构建,绝不对外暴露为 Bean
/// (防挤退 Boot 主 jdbcTemplate 自动配置,同 Sub2apiRechargeAutoConfiguration 的约定)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiUsageBoardAutoConfiguration {

    /// 用量榜只读读模型 Bean:只读 DataSource 在本方法内现场构建,不对外暴露。
    @Bean
    @ConditionalOnMissingBean
    public UsageBoardReadModel usageBoardReadModel(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = DataSourceBuilder.create()
                .url(rd.getUrl())
                .username(rd.getUsername())
                .password(rd.getPassword())
                .build();
        return new JdbcUsageBoardReadModel(new JdbcTemplate(ds));
    }
}
