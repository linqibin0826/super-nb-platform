package me.supernb.ops.app.usecase.subscription;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.subscription.command.DeleteOpsSubscriptionCommand;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 删订阅:删不到 404。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class DeleteOpsSubscriptionHandlerTest {

    final OpsSubscriptionRepository subscriptions = mock(OpsSubscriptionRepository.class);
    final DeleteOpsSubscriptionHandler handler = new DeleteOpsSubscriptionHandler(subscriptions);

    @Test
    void deletesExisting() {
        when(subscriptions.delete(9L)).thenReturn(true);
        assertThatCode(() -> handler.handle(new DeleteOpsSubscriptionCommand(9L))).doesNotThrowAnyException();
    }

    @Test
    void missingThrowsNotFound() {
        when(subscriptions.delete(9L)).thenReturn(false);
        assertThatThrownBy(() -> handler.handle(new DeleteOpsSubscriptionCommand(9L)))
                .isInstanceOf(OpsException.class).hasMessageContaining("不存在");
    }
}
