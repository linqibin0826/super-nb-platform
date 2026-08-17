package me.supernb.ops.app.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.query.view.SecretView;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 按需解密:两字段各自解密,null 保持 null;账号不存在 404。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class OpsSecretQueryServiceTest {

    static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    final OpsAccountRepository accounts = mock(OpsAccountRepository.class);
    final SecretCipher cipher = mock(SecretCipher.class);
    final OpsSecretQueryService service = new OpsSecretQueryService(accounts, cipher);

    static AccountRow row(String passwordEnc, String recoveryEnc) {
        return new AccountRow(5L, new AccountData("a@gmail.com", "gmail", passwordEnc, null, recoveryEnc,
                null, null, null, AccountStatus.ACTIVE, null, null), NOW, NOW);
    }

    @Test
    void revealDecryptsBothFields() {
        when(accounts.find(5L)).thenReturn(Optional.of(row("v1:a:b", "v1:c:d")));
        when(cipher.decrypt("v1:a:b")).thenReturn("plain1");
        when(cipher.decrypt("v1:c:d")).thenReturn("plain2");
        assertThat(service.reveal(5L)).isEqualTo(new SecretView("plain1", "plain2"));
    }

    @Test
    void nullCiphertextStaysNull() {
        when(accounts.find(5L)).thenReturn(Optional.of(row(null, null)));
        when(cipher.decrypt(null)).thenReturn(null);
        assertThat(service.reveal(5L)).isEqualTo(new SecretView(null, null));
    }

    @Test
    void missingAccountThrowsNotFound() {
        when(accounts.find(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reveal(5L)).isInstanceOf(OpsException.class);
    }
}
