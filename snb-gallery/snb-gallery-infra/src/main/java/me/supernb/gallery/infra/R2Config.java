package me.supernb.gallery.infra;

import java.net.URI;
import me.supernb.gallery.app.ImageStoragePort;
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

    @Bean
    public S3Client s3Client(GalleryR2Properties props) {
        return S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(credentials(props))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

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

    @Bean
    public ImageStoragePort imageStoragePort(S3Client s3Client, S3Presigner s3Presigner, GalleryR2Properties props) {
        return new R2StorageAdapter(s3Client, s3Presigner, props.getBucket());
    }

    private static StaticCredentialsProvider credentials(GalleryR2Properties props) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
    }
}
