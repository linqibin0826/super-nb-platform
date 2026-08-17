package me.supernb.sub2api.account;

import java.math.BigDecimal;

/// 用户当前余额只读模型(签到负余额禁签闸门用)。sub2api 计费允许透支,余额可为负。
public interface UserBalanceReadModel {

    /// 用户当前余额;查无此人(或已软删)返回 0。
    BigDecimal balance(long userId);
}
