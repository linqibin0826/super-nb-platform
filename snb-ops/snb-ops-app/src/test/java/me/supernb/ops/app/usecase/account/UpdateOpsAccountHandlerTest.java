package me.supernb.ops.app.usecase.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.account.command.UpdateOpsAccountCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.crypto.SecretCipher;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 改账号:password 为 null=保留库中原密文,非 null=重新加密覆盖;账号不存在 404。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class UpdateOpsAccountHandlerTest {

    static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    final OpsAccountRepository accounts = mock(OpsAccountRepository.class);
    final SecretCipher cipher = mock(SecretCipher.class);
    final UpdateOpsAccountHandler handler = new UpdateOpsAccountHandler(accounts, cipher);

    static AccountRow existing() {
        return new AccountRow(5L, new AccountData("a@gmail.com", "gmail", "v1:old:ct", null, "v1:old2:ct",
                null, null, null, AccountStatus.ACTIVE, null, null), NOW, NOW);
    }

    @Test
    void nullPasswordKeepsExistingCiphertext() {
        when(accounts.find(5L)).thenReturn(Optional.of(existing()));
        when(accounts.update(eq(5L), any())).thenReturn(true);
        handler.handle(new UpdateOpsAccountCommand(5L, "a@gmail.com", "gmail", null, null, null,
                "2024", "US", "林琪斌", AccountStatus.ACTIVE, null, "改备注"));
        ArgumentCaptor<AccountData> captor = ArgumentCaptor.forClass(AccountData.class);
        verify(accounts).update(eq(5L), captor.capture());
        assertThat(captor.getValue().passwordEnc()).isEqualTo("v1:old:ct");
        assertThat(captor.getValue().recoveryPasswordEnc()).isEqualTo("v1:old2:ct");
        verify(cipher, never()).encrypt(any());
    }

    @Test
    void nonNullPasswordReencrypts() {
        when(accounts.find(5L)).thenReturn(Optional.of(existing()));
        when(accounts.update(eq(5L), any())).thenReturn(true);
        when(cipher.encrypt("new")).thenReturn("v1:new:ct");
        handler.handle(new UpdateOpsAccountCommand(5L, "a@gmail.com", "gmail", "new", null, null,
                null, null, null, AccountStatus.ACTIVE, null, null));
        ArgumentCaptor<AccountData> captor = ArgumentCaptor.forClass(AccountData.class);
        verify(accounts).update(eq(5L), captor.capture());
        assertThat(captor.getValue().passwordEnc()).isEqualTo("v1:new:ct");
        assertThat(captor.getValue().recoveryPasswordEnc()).isEqualTo("v1:old2:ct");
    }

    @Test
    void missingAccountThrowsNotFound() {
        when(accounts.find(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new UpdateOpsAccountCommand(5L, "a@gmail.com", null, null,
                null, null, null, null, null, AccountStatus.ACTIVE, null, null)))
                .isInstanceOf(OpsException.class).hasMessageContaining("不存在");
        verify(accounts, never()).update(anyLong(), any());
    }

    @Test
    void rejectsMalformedEmail() {
        assertThatThrownBy(() -> handler.handle(new UpdateOpsAccountCommand(5L, "no-at", null, null,
                null, null, null, null, null, AccountStatus.ACTIVE, null, null)))
                .isInstanceOf(OpsException.class);
        verify(accounts, never()).update(anyLong(), any());
    }
}
