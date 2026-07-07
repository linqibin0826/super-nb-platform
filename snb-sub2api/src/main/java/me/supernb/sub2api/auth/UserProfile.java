package me.supernb.sub2api.auth;

/// sub2api 用户身份(introspect 结果)。防腐层对外只暴露业务需要的三个字段。
///
/// @param id     用户 id
/// @param role   角色(user / admin)
/// @param status 账号状态(active / ...)
public record UserProfile(long id, String role, String status) {

    /// 是否为可参与业务的正常账号:status 为 active 的 user **或 admin**。
    ///
    /// 放行 admin 是对齐旧 gallery-svc 的行为——站长自用账号的生成历史/互动记录都落在 admin 身份下,
    /// 2026-07-07 P2 验证时用真 token 实测,发现拒绝 admin 会造成割接回归。
    /// 充值榜等场景「排除 admin」是读模型自己的业务口径,跟这里的鉴权判定无关。
    public boolean isActiveAccount() {
        return ("user".equals(role) || "admin".equals(role)) && "active".equals(status);
    }
}
