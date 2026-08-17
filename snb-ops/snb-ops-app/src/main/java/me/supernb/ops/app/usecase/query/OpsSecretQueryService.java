package me.supernb.ops.app.usecase.query;

import me.supernb.ops.app.usecase.query.view.SecretView;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import org.springframework.stereotype.Service;

/// 按需解密:「显示密码」端点专用,列表视图永远不走这条路。
@Service
public class OpsSecretQueryService {

    private final OpsAccountRepository accounts;
    private final SecretCipher cipher;

    /// 构造:注入账号仓储与解密端口。
    public OpsSecretQueryService(OpsAccountRepository accounts, SecretCipher cipher) {
        this.accounts = accounts;
        this.cipher = cipher;
    }

    public SecretView reveal(long accountId) {
        var row = accounts.find(accountId).orElseThrow(() -> OpsException.accountNotFound(accountId));
        return new SecretView(cipher.decrypt(row.data().passwordEnc()),
                cipher.decrypt(row.data().recoveryPasswordEnc()));
    }
}
