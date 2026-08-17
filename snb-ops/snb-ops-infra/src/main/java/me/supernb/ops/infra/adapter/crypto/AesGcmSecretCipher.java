package me.supernb.ops.infra.adapter.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/// [SecretCipher] 实现:AES-256-GCM,密文 `v1:b64(nonce12):b64(ct+tag)`。
/// 密钥 env `OPS_SECRET_KEY`(base64 32 字节);缺省空值=用时抛 IllegalStateException(fail-closed 500),
/// 绝不静默存明文。留 v1 版本位方便将来换钥/换算法。
@Component
public class AesGcmSecretCipher implements SecretCipher {

    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] key; // 空数组 = 未配置

    /// 构造:接收 base64 密钥(`ops.secret-key`);空串不炸,留到使用时 fail-closed(容器要能起来)。
    public AesGcmSecretCipher(@Value("${ops.secret-key:}") String base64Key) {
        this.key = base64Key == null || base64Key.isBlank() ? new byte[0] : Base64.getDecoder().decode(base64Key);
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        requireKey();
        byte[] nonce = new byte[NONCE_LEN];
        RANDOM.nextBytes(nonce);
        byte[] ct = run(Cipher.ENCRYPT_MODE, nonce, plaintext.getBytes(StandardCharsets.UTF_8));
        return "v1:" + Base64.getEncoder().encodeToString(nonce) + ":" + Base64.getEncoder().encodeToString(ct);
    }

    @Override
    public String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        requireKey();
        String[] parts = stored.split(":", 3);
        if (parts.length != 3 || !"v1".equals(parts[0])) {
            throw new IllegalStateException("密文格式不识别(期望 v1:nonce:ct)");
        }
        byte[] nonce;
        byte[] ct;
        try {
            nonce = Base64.getDecoder().decode(parts[1]);
            ct = Base64.getDecoder().decode(parts[2]);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("密文不是合法 base64", e);
        }
        return new String(run(Cipher.DECRYPT_MODE, nonce, ct), StandardCharsets.UTF_8);
    }

    private void requireKey() {
        if (key.length == 0) {
            throw new IllegalStateException("OPS_SECRET_KEY 未配置,账号密码加解密不可用");
        }
    }

    private byte[] run(int mode, byte[] nonce, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new IllegalStateException("加解密失败(密钥不符或密文损坏)", e);
        }
    }
}
