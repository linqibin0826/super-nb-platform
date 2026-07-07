/// sub2api 鉴权防腐层:introspect 客户端 + `@CurrentUser` 参数解析器,controller 免写鉴权样板的唯一入口。
///
/// [Sub2apiIntrospectClient] 转发 Authorization 头到 sub2api `/api/v1/user/profile` 换回身份,
/// 进程内短缓存吸收同一 token 的高频复验;[CurrentUser] 标注的 [UserProfile] 方法参数由
/// [CurrentUserArgumentResolver] 解析,非 active 的 user/admin 账号统一映射 401,
/// 可用性判定唯一收在 [UserProfile#isActiveAccount()]。解析成功的画像顺带挂到请求属性,
/// 供 JPA 审计的 `AuditorAware` 取 created_by/updated_by。
///
/// 本包类型只进 infra / adapter(ArchUnit 门禁)。
package me.supernb.sub2api.auth;
