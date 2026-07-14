package me.supernb.activity.app.usecase.achievement.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import org.junit.jupiter.api.Test;

class MarkAchievementsSeenHandlerTest {

    private final AchievementUnlockPort unlockPort = mock(AchievementUnlockPort.class);
    private final MarkAchievementsSeenHandler handler = new MarkAchievementsSeenHandler(unlockPort);

    @Test
    void delegatesToUnlockPortAndReturnsAcknowledgedCount() {
        when(unlockPort.markSeen(42, List.of("api_calls_2"))).thenReturn(1);

        int acknowledged = handler.handle(new MarkAchievementsSeenCommand(42, List.of("api_calls_2")));

        assertThat(acknowledged).isEqualTo(1);
        verify(unlockPort).markSeen(42, List.of("api_calls_2"));
    }
}
