package me.supernb.gallery.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

    @Test
    void allowsUpToBurstThenDenies() {
        TokenBucket bucket = new TokenBucket(3, 60); // 容量 3,补 1/秒
        assertThat(bucket.allow("ip", 0.0)).isTrue();
        assertThat(bucket.allow("ip", 0.0)).isTrue();
        assertThat(bucket.allow("ip", 0.0)).isTrue();
        assertThat(bucket.allow("ip", 0.0)).isFalse(); // 桶空
    }

    @Test
    void refillsOverTime() {
        TokenBucket bucket = new TokenBucket(3, 60);
        for (int i = 0; i < 3; i++) {
            bucket.allow("ip", 0.0);
        }
        assertThat(bucket.allow("ip", 0.0)).isFalse();
        assertThat(bucket.allow("ip", 2.0)).isTrue(); // 2 秒补 2 枚
    }

    @Test
    void perKeyIsolation() {
        TokenBucket bucket = new TokenBucket(1, 60);
        assertThat(bucket.allow("a", 0.0)).isTrue();
        assertThat(bucket.allow("a", 0.0)).isFalse();
        assertThat(bucket.allow("b", 0.0)).isTrue(); // 另一个 key 独立
    }

    @Test
    void clientKeyTakesLastForwardedHop() {
        assertThat(TokenBucket.clientKey("1.1.1.1, 2.2.2.2", "9.9.9.9")).isEqualTo("2.2.2.2");
        assertThat(TokenBucket.clientKey(null, "9.9.9.9")).isEqualTo("9.9.9.9");
        assertThat(TokenBucket.clientKey("  ", "9.9.9.9")).isEqualTo("9.9.9.9");
    }
}
