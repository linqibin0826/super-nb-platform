package me.supernb.sub2api.autoconfig;

import javax.sql.DataSource;
import me.supernb.sub2api.invoice.InvoiceOrderReadModel;
import me.supernb.sub2api.invoice.JdbcInvoiceOrderReadModel;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/// 开票只读读模型装配:不配 `sub2api.read-datasource.url` 就完全不装配。
/// 只读 DataSource 在 @Bean 方法内部现场构建、不对外暴露(防挤退 Boot 主 jdbcTemplate,
/// 详见 Sub2apiRechargeAutoConfiguration 类注释)。
@AutoConfiguration(after = Sub2apiAutoConfiguration.class)
@ConditionalOnProperty(prefix = "sub2api.read-datasource", name = "url")
@EnableConfigurationProperties(Sub2apiProperties.class)
public class Sub2apiInvoiceAutoConfiguration {

    /// 开票只读读模型 Bean。
    @Bean
    @ConditionalOnMissingBean
    public InvoiceOrderReadModel invoiceOrderReadModel(Sub2apiProperties props) {
        Sub2apiProperties.ReadDatasource rd = props.getReadDatasource();
        DataSource ds = DataSourceBuilder.create()
                .url(rd.getUrl())
                .username(rd.getUsername())
                .password(rd.getPassword())
                .build();
        return new JdbcInvoiceOrderReadModel(new JdbcTemplate(ds));
    }
}
