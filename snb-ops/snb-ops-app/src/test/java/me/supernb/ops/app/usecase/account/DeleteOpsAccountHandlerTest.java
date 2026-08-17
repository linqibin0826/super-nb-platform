package me.supernb.ops.app.usecase.account;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.account.command.DeleteOpsAccountCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 删账号:名下有订阅一律拒(409);无订阅才删;删不到 404。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class DeleteOpsAccountHandlerTest {

    final OpsAccountRepository accounts = mock(OpsAccountRepository.class);
    final OpsSubscriptionRepository subscriptions = mock(OpsSubscriptionRepository.class);
    final DeleteOpsAccountHandler handler = new DeleteOpsAccountHandler(accounts, subscriptions);

    @Test
    void refusesWhenSubscriptionsExist() {
        when(subscriptions.countByAccount(5L)).thenReturn(2);
        assertThatThrownBy(() -> handler.handle(new DeleteOpsAccountCommand(5L)))
                .isInstanceOf(OpsException.class).hasMessageContaining("订阅");
        verify(accounts, never()).delete(anyLong());
    }

    @Test
    void deletesWhenNoSubscriptions() {
        when(subscriptions.countByAccount(5L)).thenReturn(0);
        when(accounts.delete(5L)).thenReturn(true);
        assertThatCode(() -> handler.handle(new DeleteOpsAccountCommand(5L))).doesNotThrowAnyException();
        verify(accounts).delete(5L);
    }

    @Test
    void missingAccountThrowsNotFound() {
        when(subscriptions.countByAccount(5L)).thenReturn(0);
        when(accounts.delete(5L)).thenReturn(false);
        assertThatThrownBy(() -> handler.handle(new DeleteOpsAccountCommand(5L)))
                .isInstanceOf(OpsException.class).hasMessageContaining("不存在");
    }
}
