package me.supernb.gallery.domain.port.storage;

import java.time.Duration;

/// 私有对象存储端口(gallery-infra 用 AWS SDK v2 对接 R2)。
public interface ImageStoragePort {

    /// 写一个私有对象;对象不可公开直连,只能靠 [#presignGet] 限时签发的地址访问。
    void put(String key, byte[] data, String contentType);

    /// 签一个短时效 GET URL(面向浏览器)。
    String presignGet(String key, Duration ttl);

    /// 删一个对象。
    void delete(String key);

    /// 内容 sha256(hex 编码),参考图去重的身份来源。
    String sha256(byte[] data);
}
