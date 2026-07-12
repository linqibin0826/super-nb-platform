package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 资质审核未通过(门槛/账龄不足) → 409(QUOTA_EXCEEDED 同款「资格不够」语义,NoDrawsLeft 先例)。
/// message 面向用户展示,带「还需 ¥XX」明细,由调用方拼装。
public class RaffleNotEligibleException extends DomainException {

    /// 带明细文案构造。
    public RaffleNotEligibleException(String message) {
        super(message, StandardErrorTrait.QUOTA_EXCEEDED);
    }
}
