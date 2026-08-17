package me.supernb.ops.infra.adapter.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// AES-256-GCM 密码加解密:roundtrip 一致/密文带版本位/篡改必炸/换钥解不开/空钥 fail-closed/null 透传。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class AesGcmSecretCipherTest {

    static final String KEY = Base64.getEncoder().encodeToString(new byte[32]); // 测试假钥:全零 32 字节

    static final String KEY2;

    static {
        byte[] b = new byte[32];
        Arrays.fill(b, (byte) 1);
        KEY2 = Base64.getEncoder().encodeToString(b);
    }

    final AesGcmSecretCipher cipher = new AesGcmSecretCipher(KEY);

    @Test
    void roundtripRestoresPlaintextAndCiphertextCarriesVersionPrefix() {
        String stored = cipher.encrypt("p@ss워드-中文!");
        assertThat(stored).startsWith("v1:").doesNotContain("p@ss");
        assertThat(cipher.decrypt(stored)).isEqualTo("p@ss워드-中文!");
    }

    @Test
    void samePlaintextEncryptsDifferentlyEachTime() {
        assertThat(cipher.encrypt("x")).isNotEqualTo(cipher.encrypt("x")); // 随机 nonce
    }

    @Test
    void tamperedCiphertextFailsToDecrypt() {
        String stored = cipher.encrypt("secret");
        String tampered = stored.substring(0, stored.length() - 4) + "AAAA";
        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void wrongKeyFailsToDecrypt() {
        String stored = cipher.encrypt("secret");
        assertThatThrownBy(() -> new AesGcmSecretCipher(KEY2).decrypt(stored))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void blankKeyFailsClosedOnUse() {
        AesGcmSecretCipher unconfigured = new AesGcmSecretCipher("");
        assertThatThrownBy(() -> unconfigured.encrypt("x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("OPS_SECRET_KEY");
        assertThatThrownBy(() -> unconfigured.decrypt("v1:a:b"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("OPS_SECRET_KEY");
    }

    @Test
    void nullPassesThrough() {
        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.decrypt(null)).isNull();
    }
}
