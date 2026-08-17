package me.supernb.ops.domain.port.crypto;

/// 账号密码加解密端口。密文自带格式版本位(v1:nonce:ct),实现在 infra(AES-256-GCM)。
/// null 入 null 出——密码本就是可空字段,由调用方决定语义。
public interface SecretCipher {

    String encrypt(String plaintext);

    String decrypt(String stored);
}
