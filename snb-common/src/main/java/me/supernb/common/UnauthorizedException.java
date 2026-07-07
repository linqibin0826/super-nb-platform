package me.supernb.common;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 未登录 / 身份无效(非 active 的 user/admin 账号)。映射 HTTP 401(UNAUTHORIZED)。
///
/// 住 snb-common、不归任何一个限界上下文的 domain——鉴权是跨上下文能力,
/// `@CurrentUser` 解析失败(snb-sub2api 的 CurrentUserArgumentResolver)统一抛这一个类型,
/// 各上下文共用同一份 401,不各自定义。
public class UnauthorizedException extends DomainException {

    /// 401:未携带有效凭证或用户不可用。
    public UnauthorizedException() {
        super("未登录或身份无效", StandardErrorTrait.UNAUTHORIZED);
    }
}
