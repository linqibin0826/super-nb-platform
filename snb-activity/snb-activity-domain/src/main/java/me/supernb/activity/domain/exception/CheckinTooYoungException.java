package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 账龄不足 24 小时 → 403(FORBIDDEN——已认证用户的访问被禁止,语义精确匹配"账号存在但资历不够";
/// 前端接线计划契约明确要求 403,不是 QUOTA_EXCEEDED 对应的 429)。
public class CheckinTooYoungException extends DomainException {

    /// 固定文案构造。
    public CheckinTooYoungException() {
        super("账号注册需满 24 小时才能签到", StandardErrorTrait.FORBIDDEN);
    }
}
