package me.supernb.ops.adapter.rest;

import me.supernb.ops.domain.exception.OpsException;
import me.supernb.sub2api.auth.UserProfile;

/// admin 守卫与 id 解析(三控制器共用)。
final class OpsAdminGuard {

    private OpsAdminGuard() {
    }

    static void requireAdmin(UserProfile user) {
        if (!user.isAdmin()) {
            throw OpsException.adminRequired();
        }
    }

    static long idAsLong(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw OpsException.invalidInput("id 不是合法数字: " + id);
        }
    }
}
