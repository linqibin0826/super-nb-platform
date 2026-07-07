package me.supernb.sub2api.auth;

/// sub2api 用户身份(introspect 结果)。防腐层对外只暴露业务需要的三字段。
///
/// @param id     用户 id
/// @param role   角色(user / admin)
/// @param status 状态(active / ...)
public record UserProfile(long id, String role, String status) {

    /// 是否为可参与业务的正常终端用户(role=user 且 status=active)。
    public boolean isActiveUser() {
        return "user".equals(role) && "active".equals(status);
    }
}
