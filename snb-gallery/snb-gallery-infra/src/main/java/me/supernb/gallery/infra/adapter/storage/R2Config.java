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

/// gallery R2(S3 兼容对象存储)装配:构建 S3Client/S3Presigner,组装成 [ImageStoragePort]。
///
/// 仅当 `gallery.r2.endpoint` 配置项存在才激活;缺配时(如部分测试场景)本类不装配,
/// 改由测试自行提供 mock ImageStoragePort。⚠️ application.yml 不能给这个 key 兜空串默认值——
/// `@ConditionalOnProperty` 只看"是否存在",空串也算存在,会误激活这层装配,
/// 后面 `URI.create` 拿空串当 endpoint 直接 NPE。
@Configuration
@EnableConfigurationProperties(GalleryR2Properties.class)
@ConditionalOnProperty(prefix = "gallery.r2", name = "endpoint")
public class R2Config {

    /// R2 S3 客户端(path-style 寻址),供端口的写/删用。
    @Bean
    public S3Client s3Client(GalleryR2Properties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    /// presigned URL 签名器,endpoint 优先取面向浏览器的 publicEndpoint,未配则回落 endpoint。
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

    /// 组装 [ImageStoragePort] 实现:注入 S3 客户端/签名器与桶名,手工 new 出 R2StorageAdapter
    /// (它未标 `@Component`,只经这个 `@Bean` 方法装配)。
    @Bean
    public ImageStoragePort imageStoragePort(S3Client s3Client, S3Presigner s3Presigner, GalleryR2Properties props) {
        return new R2StorageAdapter(s3Client, s3Presigner, props.getBucket());
    }

    /// 构建静态凭据;access/secret key 均只经环境变量注入。
    private static StaticCredentialsProvider credentials(GalleryR2Properties props) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
    }
}
