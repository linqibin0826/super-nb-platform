package me.supernb.gallery.infra.adapter.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/// gallery 私有 R2 配置(前缀 gallery.r2.*)。凭据只走 env,绝不入库。
@ConfigurationProperties("gallery.r2")
public class GalleryR2Properties {

    private String endpoint;
    /// 面向浏览器的 endpoint(presign 专用;本地 minio 容器内外 host 不同)。空则回落 endpoint。
    private String publicEndpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
