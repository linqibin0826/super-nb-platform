package me.supernb.sub2api.auth;

/// sub2api 用户身份(introspect 结果)。防腐层对外只暴露业务需要的三字段。
///
/// @param id     用户 id
/// @param role   角色(user / admin)
/// @param status 状态(active / ...)
public record UserProfile(long id, String role, String status) {

    /// 是否为可参与业务的正常账号(status=active 的 user **或 admin**)。
    ///
    /// admin 放行对齐旧 gallery-svc 行为(站长自用账号的生成历史/互动都记在 admin 名下,
    /// 2026-07-07 P2 验证时用真 token 实测发现拒 admin 会造成割接回归);
    /// 充值榜等「排除 admin」是读模型的业务口径,与鉴权无关。
    public boolean isActiveAccount() {
        return ("user".equals(role) || "admin".equals(role)) && "active".equals(status);
    }
}
