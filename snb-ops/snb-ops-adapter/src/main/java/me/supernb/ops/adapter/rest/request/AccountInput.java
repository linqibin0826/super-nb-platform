package me.supernb.ops.adapter.rest.request;

import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;

/// 账号入参(status 非法值 → 422;password null=不改/建号时不设)。
public record AccountInput(String email, String provider, String password, String recoveryEmail,
                           String recoveryPassword, String regYear, String country, String owner,
                           String status, String source, String notes) {

    /// status 解析:null → UNVERIFIED,非法值 422。
    public AccountStatus statusOrDefault() {
        if (status == null || status.isBlank()) {
            return AccountStatus.UNVERIFIED;
        }
        try {
            return AccountStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw OpsException.invalidInput("账号状态非法: " + status);
        }
    }
}
