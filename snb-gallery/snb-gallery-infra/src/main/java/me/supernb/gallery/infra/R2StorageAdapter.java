package me.supernb.gallery.infra;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import me.supernb.gallery.app.ImageStoragePort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/// ImageStoragePort 实现:AWS SDK v2 对接私有 R2(S3 兼容)。put/delete 走 S3Client,presign 走 S3Presigner。
public class R2StorageAdapter implements ImageStoragePort {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public R2StorageAdapter(S3Client s3, S3Presigner presigner, String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    @Override
    public void put(String key, byte[] data, String contentType) {
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(data));
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build())
                .url().toString();
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    @Override
    public String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                out[i * 2] = HEX[(digest[i] >> 4) & 0xf];
                out[i * 2 + 1] = HEX[digest[i] & 0xf];
            }
            return new String(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
