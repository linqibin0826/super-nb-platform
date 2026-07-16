package me.supernb.guide.domain.model;

import java.util.regex.Pattern;
import me.supernb.guide.domain.exception.GuideException;

/// 引导 key 规则:站点.场景.版本 惯例(如 invoice.intro.v1),小写字母数字与 ._- ,最长 64。
/// 通用引导服务的唯一领域约束——key 是各前端站点自declare 的命名空间,服务端只把格式关。
public final class GuideKey {

    private static final Pattern VALID = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    private GuideKey() {
    }

    /// 校验并原样返回;不合法抛 422。
    public static String checked(String key) {
        if (key == null || !VALID.matcher(key).matches()) {
            throw GuideException.invalidKey(String.valueOf(key));
        }
        return key;
    }
}
