package me.supernb.activity.app.usecase.achievement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.LeaderboardAchievementSignalPort;
import org.junit.jupiter.api.Test;

class LeaderboardMetricSyncJobTest {

    private final LeaderboardAchievementSignalPort signalPort = mock(LeaderboardAchievementSignalPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);

    private LeaderboardMetricSyncJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new LeaderboardMetricSyncJob(signalPort, metricPort, settlementProperties);
    }

    @Test
    void skipsWhenScanDisabled() {
        job(false).syncDaily();
        verify(signalPort, never()).bestRankEver();
    }

    @Test
    void writesBestRankAsBatch() {
        when(signalPort.bestRankEver())
                .thenReturn(List.of(new LeaderboardAchievementSignalPort.UserRank(42L, 7)));
        job(true).syncDaily();
        verify(metricPort).upsertBatch("leaderboard_best_rank_ever", Map.of(42L, 7.0));
    }
}
