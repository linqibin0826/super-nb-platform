package me.supernb.activity.domain.port.read;

import java.util.List;

/// 账号年龄候选发现只读端口(sub2api 主库):`account_anniv` 成就(判定时现算,不入
/// `user_metric`)的候选用户发现——"今天注册满 N 天的用户是谁"。
public interface AccountAnniversaryReadPort {

    /// 距今恰好 days 天注册的用户 id(Asia/Shanghai 自然日边界)。
    List<Long> registeredExactlyDaysAgo(int days);
}
