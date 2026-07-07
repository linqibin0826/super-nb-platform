package me.supernb.common;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 未登录 / 身份无效(非 active 终端用户)。映射 HTTP 401。跨上下文共用。
public class UnauthorizedException extends DomainException {

    public UnauthorizedException() {
        super("未登录或身份无效", StandardErrorTrait.UNAUTHORIZED);
    }
}
