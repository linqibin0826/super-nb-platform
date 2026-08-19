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

    /// 邮箱格式校验(账号即邮箱)。只含 @ 不够——2026-08-18 生产真放进过
    /// 粘贴带尾竖线的「…gmail.com|」造出重复行,故收紧为务实正则:
    /// local 允许字母数字与 ._%+-,域名须带 TLD。台账真实形状见测试。
    private static final java.util.regex.Pattern EMAIL = java.util.regex.Pattern
            .compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    static String requireEmail(String email) {
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw OpsException.invalidInput("邮箱必填且须是邮箱格式");
        }
        return email.trim();
    }
}
