package me.supernb.activity.app.usecase.campaign.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.PoolPort;
import me.supernb.activity.domain.port.RechargeQueryPort;
import org.junit.jupiter.api.Test;

/// 无进行中活动时,公开查询用例一律优雅降级为空(不抛异常)。
class GracefulDegradationTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final RechargeQueryPort rechargePort = mock(RechargeQueryPort.class);
    private final PoolPort poolPort = mock(PoolPort.class);

    @Test
    void allPublicQueriesEmptyWhenNoCampaign() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());

        assertThat(new GetLeaderboardUseCase(campaignPort, rechargePort).leaderboard()).isEmpty();
        assertThat(new GetRecentRechargesUseCase(campaignPort, rechargePort).recentRecharges()).isEmpty();
        assertThat(new GetPoolUseCase(campaignPort, poolPort).pool()).isEmpty();
    }
}
