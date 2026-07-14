package me.supernb.activity.app.usecase.achievement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import me.supernb.activity.domain.port.read.ReferralAchievementSignalPort;
import org.junit.jupiter.api.Test;

/// "有效"= 被邀者的 api_call_total_count ≥1(决策④);零有效邀请人不写入 metric 行。
class ReferralMetricSyncJobTest {

    private final ReferralAchievementSignalPort signalPort = mock(ReferralAchievementSignalPort.class);
    private final UserMetricPort metricPort = mock(UserMetricPort.class);

    private ReferralMetricSyncJob job(boolean scanEnabled) {
        CheckinSettlementProperties settlementProperties = new CheckinSettlementProperties(
                new BigDecimal("250"), new BigDecimal("10"), scanEnabled, false);
        return new ReferralMetricSyncJob(signalPort, metricPort, settlementProperties);
    }

    @Test
    void skipsWhenScanDisabled() {
        job(false).syncDaily();
        verify(signalPort, never()).allInviteeIdsByInviter();
    }

    @Test
    void countsOnlyInviteesWithAtLeastOneApiCall() {
        when(signalPort.allInviteeIdsByInviter()).thenReturn(Map.of(1L, List.of(10L, 11L, 12L)));
        when(metricPort.value(10, "api_call_total_count")).thenReturn(Optional.of(5.0));
        when(metricPort.value(11, "api_call_total_count")).thenReturn(Optional.empty()); // 从未调用
        when(metricPort.value(12, "api_call_total_count")).thenReturn(Optional.of(1.0));

        job(true).syncDaily();

        verify(metricPort).upsertBatch("referral_valid_count", Map.of(1L, 2.0));
    }

    @Test
    void invitersWithZeroValidInviteesAreOmittedFromBatch() {
        when(signalPort.allInviteeIdsByInviter()).thenReturn(Map.of(2L, List.of(20L)));
        when(metricPort.value(20, "api_call_total_count")).thenReturn(Optional.empty());
        job(true).syncDaily();
        verify(metricPort).upsertBatch("referral_valid_count", Map.of());
    }
}
