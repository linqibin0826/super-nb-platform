package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 用户当前无剩余抽奖次数。映射 HTTP 409(QUOTA_EXCEEDED)。
public class NoDrawsLeftException extends DomainException {

    /// 409:无剩余抽奖次数或奖池不足。
    public NoDrawsLeftException() {
        super("无剩余抽奖次数", StandardErrorTrait.QUOTA_EXCEEDED);
    }
}
