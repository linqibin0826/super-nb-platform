package me.supernb.gallery.infra.adapter.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/// gallery 私有 R2 配置(`gallery.r2.*`)。全部字段只经环境变量注入,凭据绝不入库(开源仓库红线)。
@Getter
@Setter
@ConfigurationProperties("gallery.r2")
public class GalleryR2Properties {

    /// R2 S3 兼容 API 的 endpoint(S3Client 读写用;publicEndpoint 未配时,presign 也回落用这个)。
    private String endpoint;

    /// 面向浏览器的 endpoint,仅 presign 生成的 URL 用;本地开发 minio 容器内外可达地址不同,
    /// 需单独配一个浏览器能打的地址。留空则回落 `endpoint`。
    private String publicEndpoint;

    /// 桶名。
    private String bucket;

    /// R2 access key,仅经环境变量注入。
    private String accessKey;

    /// R2 secret key,仅经环境变量注入。
    private String secretKey;
}
