package me.supernb.sub2api.autoconfig;

import javax.sql.DataSource;
import me.supernb.sub2api.usage.JdbcUsageIncrementReadModel;
import me.supernb.sub2api.usage.UsageIncrementReadModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/// 用量增量只读读模型装配:不配 `sub2api.read-datasource.url` 就完全不装配
/// (照家族条件装配惯例)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiUsageIncrementAutoConfiguration {

    /// 用量增量只读读模型 Bean:只读 DataSource 在方法内现场构建,不对外暴露为 Bean
    /// (防挤退 Boot 主 jdbcTemplate,同 Sub2apiRechargeAutoConfiguration 类注释)。
    @Bean
    @ConditionalOnMissingBean
    public UsageIncrementReadModel usageIncrementReadModel(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = rd.build();
        return new JdbcUsageIncrementReadModel(new JdbcTemplate(ds));
    }
}
