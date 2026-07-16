package me.supernb.invoice.app.usecase.profile;

import java.util.Locale;

/// 税号格式验真(纯本地零成本,与前端 `src/invoice/taxno.ts` 同一套规则,改必两边同步):
/// - 18 位统一社会信用代码:GB 32100-2015 限定字符集(无 I/O/S/V/Z)+ 末位加权 mod31 校验码,
///   能把「抄错一位」「随手乱填」全部挡死——这是重开发票的头号成因;
/// - 15 位纯数字:三证合一前的老税务登记证号,存量放行;
/// - 其余长度/字符一律拒。校验的是「格式与校验位」,不验「该企业是否存在」(那是第三方核验的事)。
final class TaxNoFormat {

    /// GB 32100 字符表,下标即字符代码值(0-30)。
    private static final String ALPHABET = "0123456789ABCDEFGHJKLMNPQRTUWXY";

    /// 前 17 位的加权因子。
    private static final int[] WEIGHTS = {1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28};

    private TaxNoFormat() {
    }

    /// 是否合法税号(大小写不敏感;null/空白为不合法——是否必填由调用方裁决)。
    static boolean isValid(String raw) {
        if (raw == null) {
            return false;
        }
        String value = raw.strip().toUpperCase(Locale.ROOT);
        if (value.length() == 15) {
            return value.chars().allMatch(Character::isDigit);
        }
        if (value.length() != 18) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int code = ALPHABET.indexOf(value.charAt(i));
            if (code < 0) {
                return false;
            }
            sum += code * WEIGHTS[i];
        }
        // GB 32100:C18 = 31 − (Σ mod 31),Σ 整除时取 0
        int expected = (31 - sum % 31) % 31;
        return ALPHABET.indexOf(value.charAt(17)) == expected;
    }
}
