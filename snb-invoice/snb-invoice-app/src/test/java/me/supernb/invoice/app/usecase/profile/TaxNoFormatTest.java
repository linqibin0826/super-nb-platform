package me.supernb.invoice.app.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 税号格式验真:真实 18 位号过校验位、抄错一位即拒、字符集把门、15 位老号纯数字放行。
/// 测试向量与前端 taxno.spec.ts 同源,改必两边同步。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TaxNoFormatTest {

    @Test
    void realUsccNumbersPass() {
        assertThat(TaxNoFormat.isValid("9144030071526726XG")).isTrue();  // 腾讯科技(深圳)
        assertThat(TaxNoFormat.isValid("91440300708461136T")).isTrue();  // 深圳市腾讯计算机系统
        assertThat(TaxNoFormat.isValid("  9144030071526726xg  ")).isTrue(); // 大小写/空白不敏感
    }

    @Test
    void singleFlippedCharRejected() {
        assertThat(TaxNoFormat.isValid("9144030071526726XA")).isFalse(); // 末位校验码不符
        assertThat(TaxNoFormat.isValid("9144030071526726YG")).isFalse(); // 中途抄错一位
    }

    @Test
    void forbiddenCharsetRejected() {
        assertThat(TaxNoFormat.isValid("9I440300708461136T")).isFalse(); // I 不在国标字符集
        assertThat(TaxNoFormat.isValid("91440300708461136O")).isFalse(); // O 同理
    }

    @Test
    void garbageAndWrongLengthsRejected() {
        assertThat(TaxNoFormat.isValid("fdsa")).isFalse();
        assertThat(TaxNoFormat.isValid("")).isFalse();
        assertThat(TaxNoFormat.isValid(null)).isFalse();
        assertThat(TaxNoFormat.isValid("9144030071526726X")).isFalse();   // 17 位
        assertThat(TaxNoFormat.isValid("9144030071526726XG9")).isFalse(); // 19 位
    }

    @Test
    void legacyFifteenDigitAccepted() {
        assertThat(TaxNoFormat.isValid("123456789012345")).isTrue();   // 老税务登记证:15 位纯数字
        assertThat(TaxNoFormat.isValid("12345678901234A")).isFalse();  // 带字母不算老号
    }
}
