package me.supernb.ops.app.usecase.account.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.ops.domain.model.AccountStatus;

/// 改账号命令。password/recoveryPassword 为 null=保留库中原密文,非 null=重新加密覆盖。
public record UpdateOpsAccountCommand(long id, String email, String provider, String password,
                                      String recoveryEmail, String recoveryPassword, String regYear,
                                      String country, String owner, AccountStatus status,
                                      String source, String notes)
        implements Command<Void> {
}
