package me.supernb.ops.app.usecase.account;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.ops.app.usecase.account.command.CreateOpsAccountCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import org.springframework.stereotype.Service;

/// 建账号用例:email 必填含 @;密码在此处加密,仓储只见密文;status 缺省 UNVERIFIED。
@Service
public class CreateOpsAccountHandler implements CommandHandler<CreateOpsAccountCommand, String> {

    private final OpsAccountRepository accounts;
    private final SecretCipher cipher;

    /// 构造:注入账号仓储与加密端口。
    public CreateOpsAccountHandler(OpsAccountRepository accounts, SecretCipher cipher) {
        this.accounts = accounts;
        this.cipher = cipher;
    }

    @Override
    public String handle(CreateOpsAccountCommand cmd) {
        String email = requireEmail(cmd.email());
        return String.valueOf(accounts.create(new AccountData(email, cmd.provider(),
                cipher.encrypt(cmd.password()), cmd.recoveryEmail(), cipher.encrypt(cmd.recoveryPassword()),
                cmd.regYear(), cmd.country(), cmd.owner(),
                cmd.status() == null ? AccountStatus.UNVERIFIED : cmd.status(),
                cmd.source(), cmd.notes())));
    }

    /// 邮箱格式底线校验(账号即邮箱,没有 @ 就不是账号)。
    static String requireEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw OpsException.invalidInput("邮箱必填且须是邮箱格式");
        }
        return email.trim();
    }
}
