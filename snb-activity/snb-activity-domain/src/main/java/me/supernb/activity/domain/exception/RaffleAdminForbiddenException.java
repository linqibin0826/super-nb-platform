package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 抽奖管理端点要求 admin 角色 → 403。
public class RaffleAdminForbiddenException extends DomainException {
    public RaffleAdminForbiddenException() {
        super("需要管理员权限", StandardErrorTrait.FORBIDDEN);
    }
}
