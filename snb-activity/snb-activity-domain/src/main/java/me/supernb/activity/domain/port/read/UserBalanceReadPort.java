package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;

/// 用户当前余额只读端口(签到负余额禁签闸门用,2026-08-17 站长拍板:欠网费不能上机)。
/// 计费透支会把 sub2api 侧余额打到负数;此时签到一律 403——既是产品立意(欠费不发福利),
/// 也从源头避免「打卡成功、返网费却被上游负余额保护挡下」的半残台账。
public interface UserBalanceReadPort {

    /// 用户当前余额(CNY 名义额度)。查无此人返回 0(账龄闸在前,正常流程到不了这里)。
    BigDecimal balance(long userId);
}
