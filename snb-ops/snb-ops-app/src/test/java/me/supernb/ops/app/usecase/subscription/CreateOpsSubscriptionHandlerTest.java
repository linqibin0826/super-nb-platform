package me.supernb.ops.app.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.subscription.command.CreateOpsSubscriptionCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 建订阅:账号必须存在;service/status 必填;BANNED 必带封号时间;refundStatus 空默认 NONE。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CreateOpsSubscriptionHandlerTest {

    static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    final OpsAccountRepository accounts = mock(OpsAccountRepository.class);
    final OpsSubscriptionRepository subscriptions = mock(OpsSubscriptionRepository.class);
    final CreateOpsSubscriptionHandler handler = new CreateOpsSubscriptionHandler(accounts, subscriptions);

    static AccountRow account() {
        return new AccountRow(5L, new AccountData("a@gmail.com", "gmail", null, null, null,
                null, null, null, AccountStatus.ACTIVE, null, null), NOW, NOW);
    }

    static SubscriptionData data(SubService service, SubStatus status, Instant bannedAt, RefundStatus refund) {
        return new SubscriptionData(5L, service, null, null, null, null, null, null,
                null, null, null, null, status, null, null, bannedAt, null,
                refund, null, null, null, null, null, null);
    }

    @Test
    void bannedStatusRequiresBannedAt() {
        when(accounts.find(5L)).thenReturn(Optional.of(account()));
        assertThatThrownBy(() -> handler.handle(new CreateOpsSubscriptionCommand(
                data(SubService.CHATGPT, SubStatus.BANNED, null, RefundStatus.NONE))))
                .isInstanceOf(OpsException.class).hasMessageContaining("封号时间");
        verify(subscriptions, never()).create(any());
    }

    @Test
    void accountMustExist() {
        when(accounts.find(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new CreateOpsSubscriptionCommand(
                data(SubService.CHATGPT, SubStatus.FREE, null, RefundStatus.NONE))))
                .isInstanceOf(OpsException.class).hasMessageContaining("账号不存在");
    }

    @Test
    void serviceAndStatusRequired() {
        when(accounts.find(5L)).thenReturn(Optional.of(account()));
        assertThatThrownBy(() -> handler.handle(new CreateOpsSubscriptionCommand(
                data(null, SubStatus.FREE, null, RefundStatus.NONE)))).isInstanceOf(OpsException.class);
        assertThatThrownBy(() -> handler.handle(new CreateOpsSubscriptionCommand(
                data(SubService.CLAUDE, null, null, RefundStatus.NONE)))).isInstanceOf(OpsException.class);
    }

    @Test
    void happyPathDefaultsRefundStatusToNoneAndReturnsStringId() {
        when(accounts.find(5L)).thenReturn(Optional.of(account()));
        when(subscriptions.create(any())).thenReturn(11L);
        assertThat(handler.handle(new CreateOpsSubscriptionCommand(
                data(SubService.CHATGPT, SubStatus.FREE, null, null)))).isEqualTo("11");
        ArgumentCaptor<SubscriptionData> captor = ArgumentCaptor.forClass(SubscriptionData.class);
        verify(subscriptions).create(captor.capture());
        assertThat(captor.getValue().refundStatus()).isEqualTo(RefundStatus.NONE);
    }

    @Test
    void bannedWithTimeIsAccepted() {
        when(accounts.find(5L)).thenReturn(Optional.of(account()));
        when(subscriptions.create(any())).thenReturn(12L);
        assertThat(handler.handle(new CreateOpsSubscriptionCommand(
                data(SubService.CLAUDE, SubStatus.BANNED, NOW, RefundStatus.PENDING)))).isEqualTo("12");
    }
}
