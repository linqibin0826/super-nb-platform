package me.supernb.sub2api.autoconfig;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// sub2api 防腐层配置(前缀 sub2api.*)。
@Getter
@Setter
@ConfigurationProperties("sub2api")
public class Sub2apiProperties {

    /// sub2api 基址(introspect 转发目标)。
    private String baseUrl = "http://localhost:8080";

    /// introspect 结果进程内缓存秒数。
    private int introspectCacheSeconds = 30;

    /// sub2api 只读 DataSource(独立只读角色,仅 SELECT)。
    private final ReadDatasource readDatasource = new ReadDatasource();

    /// 只读源连接参数(不配 url 则充值读模型能力不装配)。
    @Getter
    @Setter
    public static class ReadDatasource {
        private String url;
        private String username;
        private String password;
    }
}
