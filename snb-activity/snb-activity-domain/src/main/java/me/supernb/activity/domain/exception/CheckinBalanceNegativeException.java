package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 余额欠费(计费透支为负)→ 403(2026-08-17 站长拍板:欠网费不能上机签到;
/// 语义同 [CheckinRechargeRequiredException]——已认证用户资格不够)。
public class CheckinBalanceNegativeException extends DomainException {

    /// 文案由调用方带上实际欠费金额组好传入。
    public CheckinBalanceNegativeException(String message) {
        super(message, StandardErrorTrait.FORBIDDEN);
    }
}
