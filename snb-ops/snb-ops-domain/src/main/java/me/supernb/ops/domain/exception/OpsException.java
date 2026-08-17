package me.supernb.ops.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// ops 领域异常:trait 由 commons 引擎映射 HTTP(404/403/409/422)。
public class OpsException extends DomainException {

    private OpsException(String message, StandardErrorTrait trait) {
        super(message, trait);
    }

    public static OpsException accountNotFound(long id) {
        return new OpsException("账号不存在: " + id, StandardErrorTrait.NOT_FOUND);
    }

    public static OpsException subscriptionNotFound(long id) {
        return new OpsException("订阅不存在: " + id, StandardErrorTrait.NOT_FOUND);
    }

    public static OpsException adminRequired() {
        return new OpsException("需要管理员身份", StandardErrorTrait.FORBIDDEN);
    }

    public static OpsException invalidInput(String detail) {
        return new OpsException("入参不合法: " + detail, StandardErrorTrait.RULE_VIOLATION);
    }

    public static OpsException duplicateEmail(String email) {
        return new OpsException("邮箱已存在: " + email, StandardErrorTrait.CONFLICT);
    }

    public static OpsException duplicateSubscription(String service) {
        return new OpsException("该邮箱已有 " + service + " 订阅行,请直接编辑", StandardErrorTrait.CONFLICT);
    }

    public static OpsException accountHasSubscriptions(long id) {
        return new OpsException("账号名下还有订阅行,先删订阅再删账号: " + id, StandardErrorTrait.CONFLICT);
    }
}
