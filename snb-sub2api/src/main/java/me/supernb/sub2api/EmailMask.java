package me.supernb.sub2api;

/// 邮箱脱敏的全站唯一口径(充值榜/用量榜/拉新榜/发布会四处读模型共用)。
///
/// 本地部分**始终保证至少遮住 2 位**:≤3 位整段遮(`***`)、4~5 位露首尾各 1 位、≥6 位露首尾各 2 位。
/// 常见邮箱(本地部分 ≥6 位,如真实 QQ 号 9~11 位)与旧口径「前2+***+后2」显示一致,只有短本地名收紧。
///
/// 背景(2026-07-13 安全审计,runbook ai-relay `deployment/31`):旧实现 `local.length()>=2 ? substring(0,2)`
/// 对 ≤2 位本地名原样回显、5 位只遮 1 位——`ab@x.com`→`ab***@x.com` 星号前已是完整本地名,
/// 鲸鱼/中奖者邮箱可被公开榜免登录读出。四处读模型统一委托本方法,防再漂移出第五种口径。
public final class EmailMask {

    private EmailMask() {
    }

    /// 脱敏:null 原样返回;无 `@` 时视整串为本地部分。露出字符数随本地部分长度收放,恒 ≥2 位被 `***` 遮住。
    public static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        String local = at >= 0 ? email.substring(0, at) : email;
        String domain = at >= 0 ? email.substring(at) : "";
        int n = local.length();
        int reveal = n >= 6 ? 2 : (n >= 4 ? 1 : 0);
        String prefix = local.substring(0, reveal);
        String suffix = reveal == 0 ? "" : local.substring(n - reveal);
        return prefix + "***" + suffix + domain;
    }
}
