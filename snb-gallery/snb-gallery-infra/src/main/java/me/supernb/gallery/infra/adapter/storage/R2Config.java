package me.supernb.gallery.infra.adapter.storage;

import java.net.URI;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/// gallery R2 装配。仅当 gallery.r2.endpoint 配置存在时激活(缺省时——如某些测试——由测试提供 mock ImageStoragePort)。
@Configuration
@EnableConfigurationProperties(GalleryR2Properties.class)
@ConditionalOnProperty(prefix = "gallery.r2", name = "endpoint")
public class R2Config {

    /// R2 S3 客户端(path-style)。
    @Bean
    public S3Client s3Client(GalleryR2Properties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /// presigned URL 签名器(用浏览器可达的 endpoint)。
    @Bean
    public S3Presigner s3Presigner(GalleryR2Properties props) {
        String endpoint = props.getPublicEndpoint() != null && !props.getPublicEndpoint().isBlank()
                ? props.getPublicEndpoint() : props.getEndpoint();
        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /// 组装 R2 存储端口实现。
    @Bean
    public ImageStoragePort imageStoragePort(S3Client s3Client, S3Presigner s3Presigner, GalleryR2Properties props) {
        return new R2StorageAdapter(s3Client, s3Presigner, props.getBucket());
    }

    /// 静态凭据(仅 env 注入)。
    private static StaticCredentialsProvider credentials(GalleryR2Properties props) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
    }
}
