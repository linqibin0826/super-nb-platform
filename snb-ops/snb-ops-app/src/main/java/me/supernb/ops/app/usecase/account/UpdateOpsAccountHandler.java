package me.supernb.ops.app.usecase.account;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.ops.app.usecase.account.command.UpdateOpsAccountCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import org.springframework.stereotype.Service;

/// 改账号用例:密码 null=保留原密文(前端没有明文可回传,这是契约),非 null=重新加密。
@Service
public class UpdateOpsAccountHandler implements CommandHandler<UpdateOpsAccountCommand, Void> {

    private final OpsAccountRepository accounts;
    private final SecretCipher cipher;

    /// 构造:注入账号仓储与加密端口。
    public UpdateOpsAccountHandler(OpsAccountRepository accounts, SecretCipher cipher) {
        this.accounts = accounts;
        this.cipher = cipher;
    }

    @Override
    public Void handle(UpdateOpsAccountCommand cmd) {
        String email = CreateOpsAccountHandler.requireEmail(cmd.email());
        AccountRow old = accounts.find(cmd.id()).orElseThrow(() -> OpsException.accountNotFound(cmd.id()));
        String passwordEnc = cmd.password() == null ? old.data().passwordEnc() : cipher.encrypt(cmd.password());
        String recoveryEnc = cmd.recoveryPassword() == null
                ? old.data().recoveryPasswordEnc() : cipher.encrypt(cmd.recoveryPassword());
        boolean ok = accounts.update(cmd.id(), new AccountData(email, cmd.provider(), passwordEnc,
                cmd.recoveryEmail(), recoveryEnc, cmd.regYear(), cmd.country(), cmd.owner(),
                cmd.status() == null ? AccountStatus.UNVERIFIED : cmd.status(), cmd.source(), cmd.notes()));
        if (!ok) {
            throw OpsException.accountNotFound(cmd.id());
        }
        return null;
    }
}
