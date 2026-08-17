package me.supernb.ops.app.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.subscription.command.UpdateOpsSubscriptionCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 改订阅:BANNED 必带封号时间;仓储未命中 404。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class UpdateOpsSubscriptionHandlerTest {

    static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    final OpsSubscriptionRepository subscriptions = mock(OpsSubscriptionRepository.class);
    final UpdateOpsSubscriptionHandler handler = new UpdateOpsSubscriptionHandler(subscriptions);

    static SubscriptionData data(SubStatus status, Instant bannedAt) {
        return new SubscriptionData(5L, SubService.CHATGPT, null, null, null, null, null, null,
                null, null, null, null, status, null, null, bannedAt, null,
                RefundStatus.NONE, null, null, null, null, null, null);
    }

    @Test
    void bannedStatusRequiresBannedAt() {
        assertThatThrownBy(() -> handler.handle(new UpdateOpsSubscriptionCommand(9L, data(SubStatus.BANNED, null))))
                .isInstanceOf(OpsException.class).hasMessageContaining("封号时间");
        verify(subscriptions, never()).update(anyLong(), any());
    }

    @Test
    void missingSubscriptionThrowsNotFound() {
        when(subscriptions.update(eq(9L), any())).thenReturn(false);
        assertThatThrownBy(() -> handler.handle(new UpdateOpsSubscriptionCommand(9L, data(SubStatus.ACTIVE, null))))
                .isInstanceOf(OpsException.class).hasMessageContaining("不存在");
    }

    @Test
    void happyPathUpdates() {
        when(subscriptions.update(eq(9L), any())).thenReturn(true);
        assertThatCode(() -> handler.handle(new UpdateOpsSubscriptionCommand(9L, data(SubStatus.BANNED, NOW))))
                .doesNotThrowAnyException();
    }
}
