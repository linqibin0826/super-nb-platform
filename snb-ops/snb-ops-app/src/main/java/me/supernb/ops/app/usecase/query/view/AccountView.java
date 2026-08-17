package me.supernb.ops.app.usecase.query.view;

import java.time.Instant;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;

/// 账号读视图:不含任何密文/明文,密码只报「有没有」。
public record AccountView(long id, String email, String provider, boolean hasPassword, String recoveryEmail,
                          boolean hasRecoveryPassword, String regYear, String country, String owner,
                          AccountStatus status, String source, String notes, Instant createdAt) {

    /// 从仓储行装配(密文在此被剥掉)。
    public static AccountView of(AccountRow row) {
        var d = row.data();
        return new AccountView(row.id(), d.email(), d.provider(), d.passwordEnc() != null, d.recoveryEmail(),
                d.recoveryPasswordEnc() != null, d.regYear(), d.country(), d.owner(), d.status(),
                d.source(), d.notes(), row.createdAt());
    }
}
