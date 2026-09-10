package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 中转接入用户被排除在抽奖之外 → 403(已认证但没有资格;区别于 409「无剩余次数」——
/// 前端据状态码弹专门文案,不能再劝人「充值满 ¥100 解锁」)。
public class DrawExcludedException extends DomainException {

    /// 文案即站长原话:Claude Max 已经打过折了,这是给散户抽的,中转不可用。
    public DrawExcludedException() {
        super("Claude Max 已经打过折了,这是给散户抽的,中转接入不可用", StandardErrorTrait.FORBIDDEN);
    }
}
