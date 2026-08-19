package me.supernb.ops.app.usecase.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.account.command.CreateOpsAccountCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 建账号:密码经 SecretCipher 加密后才进仓储;email 缺失/无 @ 拒绝;明文绝不透传。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CreateOpsAccountHandlerTest {

    final OpsAccountRepository accounts = mock(OpsAccountRepository.class);
    final SecretCipher cipher = mock(SecretCipher.class);
    final CreateOpsAccountHandler handler = new CreateOpsAccountHandler(accounts, cipher);

    static CreateOpsAccountCommand cmd(String email, String password) {
        return new CreateOpsAccountCommand(email, "gmail", password, null, null,
                "2024", "US", "林琪斌", AccountStatus.UNVERIFIED, null, null);
    }

    @Test
    void encryptsPasswordBeforePersisting() {
        when(cipher.encrypt("plain")).thenReturn("v1:n:c");
        when(cipher.encrypt(null)).thenReturn(null);
        when(accounts.create(any())).thenReturn(7L);
        assertThat(handler.handle(cmd("a@gmail.com", "plain"))).isEqualTo("7");
        ArgumentCaptor<AccountData> captor = ArgumentCaptor.forClass(AccountData.class);
        verify(accounts).create(captor.capture());
        assertThat(captor.getValue().passwordEnc()).isEqualTo("v1:n:c");
        assertThat(captor.getValue().email()).isEqualTo("a@gmail.com");
    }

    @Test
    void rejectsMissingOrMalformedEmail() {
        assertThatThrownBy(() -> handler.handle(cmd(null, null))).isInstanceOf(OpsException.class);
        assertThatThrownBy(() -> handler.handle(cmd("  ", null))).isInstanceOf(OpsException.class);
        assertThatThrownBy(() -> handler.handle(cmd("no-at-sign", null))).isInstanceOf(OpsException.class);
        // 2026-08-18 生产真踩过:粘贴带尾竖线的「tautvisbelekas16@gmail.com|」被放进库成重复行
        assertThatThrownBy(() -> handler.handle(cmd("tautvisbelekas16@gmail.com|", null)))
                .isInstanceOf(OpsException.class);
        assertThatThrownBy(() -> handler.handle(cmd("a b@gmail.com", null))).isInstanceOf(OpsException.class);
        assertThatThrownBy(() -> handler.handle(cmd("a@b", null))).isInstanceOf(OpsException.class);
        verifyNoInteractions(accounts);
    }

    @Test
    void acceptsRealLedgerEmailShapes() {
        when(cipher.encrypt(null)).thenReturn(null);
        when(accounts.create(any())).thenReturn(9L);
        // 台账在册的几种真实形状:Apple 中继/local 带点带连字符/自有域名
        handler.handle(cmd("2rszszkn6y@privaterelay.appleid.com", null));
        handler.handle(cmd("evamoni.ca1861@gmail.com", null));
        handler.handle(cmd("ca-john@linqibin.dev", null));
        handler.handle(cmd("  trimmed@gmail.com  ", null));
    }

    @Test
    void nullPasswordStaysNull() {
        when(cipher.encrypt(null)).thenReturn(null);
        when(accounts.create(any())).thenReturn(8L);
        handler.handle(cmd("b@gmail.com", null));
        ArgumentCaptor<AccountData> captor = ArgumentCaptor.forClass(AccountData.class);
        verify(accounts).create(captor.capture());
        assertThat(captor.getValue().passwordEnc()).isNull();
    }

    @Test
    void nullStatusDefaultsToUnverified() {
        when(accounts.create(any())).thenReturn(9L);
        handler.handle(new CreateOpsAccountCommand("c@gmail.com", null, null, null, null,
                null, null, null, null, null, null));
        ArgumentCaptor<AccountData> captor = ArgumentCaptor.forClass(AccountData.class);
        verify(accounts).create(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(AccountStatus.UNVERIFIED);
    }
}
