package me.supernb.sub2api;

import java.util.Locale;

/// 公开信息流「展示名」的全站唯一口径（充值榜 / 充值动态 / 用量榜 / 抽奖记录 / raffle 共用）。
///
/// 规则：**昵称非空白就用昵称，否则回落 [EmailMask#mask]**。
///
/// 🚨 但昵称等于邮箱、或等于邮箱**本地部分**时必须一并回落——否则等于把 `EmailMask`
/// 刚遮住的那几位原样吐回公开榜，脱敏形同虚设（2026-09-09 生产实查：16 个用户的
/// username 恰好就是自己的邮箱前缀）。比较前先 trim、且大小写不敏感。
///
/// 背景（2026-09-09）：站长要求充值榜「有昵称就显示昵称」。改之前全站有**三套**口径——
/// 用量榜 `JdbcUsageBoardReadModel.displayName`、抽奖记录 `displayNamesByIds` 各写一份
/// （两份都缺上面那道护栏），而充值榜 / 充值动态 / 拉新榜恒用脱敏邮箱。这正是 `EmailMask`
/// 当初要消灭的「口径漂移」，只是上移了一层。四处读模型统一委托本方法，防漂移出第四种口径。
public final class DisplayName {

    private DisplayName() {
    }

    /// 择名：昵称可用就用昵称（已 trim），否则脱敏邮箱。两者都缺时返回 `null`。
    public static String of(String username, String email) {
        if (username == null) {
            return EmailMask.mask(email);
        }
        String nick = username.trim();
        if (nick.isEmpty() || leaksEmail(nick, email)) {
            return EmailMask.mask(email);
        }
        return nick;
    }

    /// 昵称是否会把邮箱原样露出：整串等于邮箱，或等于 `@` 之前的本地部分。
    private static boolean leaksEmail(String nick, String email) {
        if (email == null) {
            return false;
        }
        String lower = nick.toLowerCase(Locale.ROOT);
        String mail = email.toLowerCase(Locale.ROOT);
        if (lower.equals(mail)) {
            return true;
        }
        int at = mail.indexOf('@');
        return at >= 0 && lower.equals(mail.substring(0, at));
    }
}
