package me.supernb.sub2api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// sub2api 防腐层配置(前缀 sub2api.*)。
@ConfigurationProperties("sub2api")
public class Sub2apiProperties {

    /// sub2api 基址(introspect 转发目标)。
    private String baseUrl = "http://localhost:8080";

    /// introspect 结果进程内缓存秒数。
    private int introspectCacheSeconds = 30;

    /// sub2api 只读 DataSource(独立只读角色,仅 SELECT)。
    private final ReadDatasource readDatasource = new ReadDatasource();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getIntrospectCacheSeconds() {
        return introspectCacheSeconds;
    }

    public void setIntrospectCacheSeconds(int introspectCacheSeconds) {
        this.introspectCacheSeconds = introspectCacheSeconds;
    }

    public ReadDatasource getReadDatasource() {
        return readDatasource;
    }

    public static class ReadDatasource {
        private String url;
        private String username;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
