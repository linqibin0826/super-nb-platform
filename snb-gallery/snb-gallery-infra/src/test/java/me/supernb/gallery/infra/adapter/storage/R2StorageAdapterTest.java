package me.supernb.gallery.infra.adapter.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/// R2StorageAdapter 对 S3 兼容存储(Testcontainers MinIO)的 put/presign/delete 端到端。
@Testcontainers
class R2StorageAdapterTest {

    @Container
    static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2024-01-16T16-07-38Z");

    static final String BUCKET = "snb-test";
    static R2StorageAdapter adapter;

    @BeforeAll
    static void init() {
        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(MINIO.getUserName(), MINIO.getPassword()));
        S3Configuration cfg = S3Configuration.builder().pathStyleAccessEnabled(true).build();
        URI endpoint = URI.create(MINIO.getS3URL());
        S3Client s3 = S3Client.builder()
                .endpointOverride(endpoint).region(Region.of("auto"))
                .credentialsProvider(creds).serviceConfiguration(cfg).build();
        s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(endpoint).region(Region.of("auto"))
                .credentialsProvider(creds).serviceConfiguration(cfg).build();
        adapter = new R2StorageAdapter(s3, presigner, BUCKET);
    }

    @Test
    void putThenPresignGetReturnsContent() throws Exception {
        adapter.put("k/hello.txt", "hello".getBytes(StandardCharsets.UTF_8), "text/plain");
        String url = adapter.presignGet("k/hello.txt", Duration.ofMinutes(5));

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("hello");
    }

    @Test
    void deleteRemovesObject() throws Exception {
        adapter.put("k/gone.txt", "x".getBytes(StandardCharsets.UTF_8), "text/plain");
        adapter.delete("k/gone.txt");
        String url = adapter.presignGet("k/gone.txt", Duration.ofMinutes(5));

        HttpResponse<String> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(404);
    }

    @Test
    void sha256MatchesKnownVector() {
        assertThat(adapter.sha256("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
