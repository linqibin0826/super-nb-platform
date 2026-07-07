package me.supernb.sub2api.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 标注在 controller 的 UserProfile 参数上:由 CurrentUserArgumentResolver 完成
/// 「Authorization 头 → introspect → 要求 active 的 user/admin 账号,否则 401」,免每个上下文手写鉴权样板。
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
