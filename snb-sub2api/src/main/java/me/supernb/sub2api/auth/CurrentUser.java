package me.supernb.sub2api.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// 标注在 controller 方法的 [UserProfile] 参数上,触发 [CurrentUserArgumentResolver] 完成鉴权解析——
/// Authorization 头 → introspect → 要求 active 的 user/admin 账号,否则 401,免每个上下文手写这套样板。
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
