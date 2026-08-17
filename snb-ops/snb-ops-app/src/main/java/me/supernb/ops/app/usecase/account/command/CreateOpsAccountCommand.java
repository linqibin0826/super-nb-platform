package me.supernb.ops.app.usecase.account.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.ops.domain.model.AccountStatus;

/// 建账号命令(password/recoveryPassword 为明文,加密在 Handler 内收口);返回新账号 id 字符串。
public record CreateOpsAccountCommand(String email, String provider, String password, String recoveryEmail,
                                      String recoveryPassword, String regYear, String country, String owner,
                                      AccountStatus status, String source, String notes)
        implements Command<String> {
}
