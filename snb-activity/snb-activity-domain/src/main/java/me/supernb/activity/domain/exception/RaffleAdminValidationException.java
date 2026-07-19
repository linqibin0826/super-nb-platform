package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 管理端请求字段未过校验规则 → 422。
public class RaffleAdminValidationException extends DomainException {
    public RaffleAdminValidationException(String message) {
        super(message, StandardErrorTrait.RULE_VIOLATION);
    }
}
