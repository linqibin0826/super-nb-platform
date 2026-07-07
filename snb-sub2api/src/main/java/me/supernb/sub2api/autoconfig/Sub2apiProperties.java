package me.supernb.sub2api.autoconfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    }
}
