package me.supernb.sub2api.autoconfig;

import com.zaxxer.hikari.HikariDataSource;
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
        // ⚠️ 连接池封顶:本分支 merge-base 早于 main 的连接池减半修复(ReadDatasource.build() 池上限 5),
        //   且本文件是新增文件、三方合并不会自动帮它补上修复——先手动 setMaximumPoolSize(5),
        //   即使漏了合 main 后的统一改造,也不会复现主库连接打满(曾 7 池×默认10 = 106/100)。
        //   合 main 后请统一改成 `DataSource ds = rd.build();`(与其余只读 autoconfig 一致)。
        HikariDataSource ds = DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(rd.getUrl())
                .username(rd.getUsername())
                .password(rd.getPassword())
                .build();
        ds.setMaximumPoolSize(5);
        return new JdbcInvoiceOrderReadModel(new JdbcTemplate(ds));
    }
}
