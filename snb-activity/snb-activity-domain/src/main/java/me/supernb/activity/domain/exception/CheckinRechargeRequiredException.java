package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 签到准入闸未过 → 403(spec §12,2026-07-31 站长拍板:近 30 天真实充值 ≥¥30 才能上机;
/// 语义同 [CheckinTooYoungException]——已认证用户的访问被禁止,资格不够)。
public class CheckinRechargeRequiredException extends DomainException {

    /// 文案由调用方按闸门实际参数组好传入(窗口天数/门槛金额都是 env 可调的,不写死)。
    public CheckinRechargeRequiredException(String message) {
        super(message, StandardErrorTrait.FORBIDDEN);
    }
}
