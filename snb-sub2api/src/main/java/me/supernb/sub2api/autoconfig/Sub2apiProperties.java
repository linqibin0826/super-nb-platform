package me.supernb.sub2api.autoconfig;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;

/// sub2api 防腐层的配置属性(前缀 `sub2api.*`)。
@Getter
@Setter
@ConfigurationProperties("sub2api")
public class Sub2apiProperties {

    /// sub2api 基址,introspect 请求的转发目标。
    private String baseUrl = "http://localhost:8080";

    /// introspect 结果的进程内缓存秒数。
    private int introspectCacheSeconds = 30;

    /// sub2api 只读库连接配置(独立只读数据库角色,权限仅 SELECT)。
    private final ReadDatasource readDatasource = new ReadDatasource();

    /// admin API 令牌(`x-api-key` 请求头);缺省空值时 `Sub2apiAdminAutoConfiguration` 整体不装配。
    private String adminKey;

    /// 开票资料 AI 识别用的中转 API key(admin 自有账号的 sk- key,自家消费;Bearer 头)。
    /// 缺省时 `Sub2apiChatAutoConfiguration` 整体不装配——同 adminKey,绝不给 yml 默认值。
    private String invoiceAiKey;

    /// AI 识别用模型(走 /v1/chat/completions);默认 gpt-5.6-luna(轻量快,实测单次 ~400 tokens)。
    private String invoiceAiModel = "gpt-5.6-luna";

    /// 只读源连接参数;`url` 缺配则充值读模型能力整体不装配。
    @Getter
    @Setter
    public static class ReadDatasource {
        /// JDBC 连接串。
        private String url;
        /// 只读账号用户名。
        private String username;
        /// 只读账号密码。
        private String password;
        /// 每个只读读模型独立持有一个连接池;上限默认 5。原走 HikariCP 默认 10,
        /// 7 个只读池 × 10 曾把 sub2api 主库连接打满(实测 106/100),2026-07-16 站长拍板每池减半。
        private int maxPoolSize = 5;

        /// 现场构建只读 DataSource(内嵌 HikariCP,池上限 maxPoolSize)。各 autoconfig 的
        /// @Bean 方法调用它,拿到的是方法内局部 DataSource——绝不作为 Bean 暴露(防挤退主 jdbcTemplate)。
        public DataSource build() {
            HikariDataSource ds = DataSourceBuilder.create()
                    .type(HikariDataSource.class)
                    .url(url)
                    .username(username)
                    .password(password)
                    .build();
            ds.setMaximumPoolSize(maxPoolSize);
            return ds;
        }
    }
}
