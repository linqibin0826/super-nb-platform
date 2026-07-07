package me.supernb.gallery.infra.adapter.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// gallery 私有 R2 配置(前缀 gallery.r2.*)。凭据只走 env,绝不入库。
@Getter
@Setter
@ConfigurationProperties("gallery.r2")
public class GalleryR2Properties {

    private String endpoint;

    /// 面向浏览器的 endpoint(presign 专用;本地 minio 容器内外 host 不同)。空则回落 endpoint。
    private String publicEndpoint;

    private String bucket;
    private String accessKey;
    private String secretKey;
}
