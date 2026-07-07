package me.supernb.gallery.infra.adapter.storage;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/// [ImageStoragePort] 实现:AWS SDK v2 对接私有 R2(S3 兼容存储)。写删走 S3Client,presign 走 S3Presigner。
public class R2StorageAdapter implements ImageStoragePort {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    /// 构造:注入 S3 客户端、签名器与桶名。
    public R2StorageAdapter(S3Client s3, S3Presigner presigner, String bucket) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    /// 写入一个对象(原始字节 + content type)。
    @Override
    public void put(String key, byte[] data, String contentType) {
        s3.putObject(
                PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
                RequestBody.fromBytes(data));
    }

    /// 签一个 ttl 时效内可访问的下载 URL。
    @Override
    public String presignGet(String key, Duration ttl) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder().signatureDuration(ttl).getObjectRequest(get).build())
                .url().toString();
    }

    /// 删除一个对象;key 本就不存在也视为成功(S3 delete 语义天然幂等)。
    @Override
    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /// 内容 sha256 摘要,hex 编码(参考图按内容去重的身份来源)。
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
            // SHA-256 是 JDK 强制实现的摘要算法,标准 JVM 上不会真的抛出;这里只是把受检异常转成非受检
            throw new IllegalStateException(e);
        }
    }
}
