package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 用户在当前活动期已无剩余抽奖次数。携带 QUOTA_EXCEEDED 语义特征,经 commons 统一
/// 错误处理映射为 HTTP 409。
public class NoDrawsLeftException extends DomainException {

    /// 409:充值换算的应得抽奖次数已抽满。奖池打空不算在内——那条路径走安慰奖,不抛异常。
    public NoDrawsLeftException() {
        super("无剩余抽奖次数", StandardErrorTrait.QUOTA_EXCEEDED);
    }
}
