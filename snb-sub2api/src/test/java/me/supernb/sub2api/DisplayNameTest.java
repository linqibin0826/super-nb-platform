package me.supernb.sub2api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DisplayNameTest {

    @Test
    void usesNicknameWhenPresent() {
        assertThat(DisplayName.of("老王", "1234567@qq.com")).isEqualTo("老王");
    }

    @Test
    void trimsNicknameEdgeWhitespace() {
        assertThat(DisplayName.of("  千早爱音  ", "1234567@qq.com")).isEqualTo("千早爱音");
    }

    @Test
    void fallsBackToMaskedEmailWhenNicknameNullOrBlank() {
        assertThat(DisplayName.of(null, "1234567@qq.com")).isEqualTo("12***67@qq.com");
        assertThat(DisplayName.of("", "1234567@qq.com")).isEqualTo("12***67@qq.com");
        assertThat(DisplayName.of("   ", "1234567@qq.com")).isEqualTo("12***67@qq.com");
    }

    /// 🚨 这两条是本类存在的理由：昵称等于邮箱、或等于邮箱本地部分时直接显示，
    /// 等于把 EmailMask 刚遮住的那几位原样吐回公开信息流（生产实查 16 个用户是这种）。
    @Test
    void fallsBackWhenNicknameEqualsEmail() {
        assertThat(DisplayName.of("1234567@qq.com", "1234567@qq.com")).isEqualTo("12***67@qq.com");
    }

    @Test
    void fallsBackWhenNicknameEqualsEmailLocalPart() {
        assertThat(DisplayName.of("1234567", "1234567@qq.com")).isEqualTo("12***67@qq.com");
        assertThat(DisplayName.of("  1234567 ", "1234567@qq.com")).isEqualTo("12***67@qq.com");
    }

    @Test
    void comparisonIgnoresCase() {
        assertThat(DisplayName.of("CrAzY33", "crazy33@gmail.com")).isEqualTo("cr***33@gmail.com");
    }

    /// 昵称只是碰巧「包含」邮箱前缀不算泄露，照常显示
    @Test
    void keepsNicknameThatMerelyContainsLocalPart() {
        assertThat(DisplayName.of("crazy33的小号", "crazy33@gmail.com")).isEqualTo("crazy33的小号");
    }

    @Test
    void handlesNullEmail() {
        assertThat(DisplayName.of("老王", null)).isEqualTo("老王");
        assertThat(DisplayName.of(null, null)).isNull();
    }
}
