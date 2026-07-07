package me.supernb.activity.app.usecase.campaign.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.read.PoolReadPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.junit.jupiter.api.Test;

/// 无进行中活动时,公开查询用例一律优雅降级为空(不抛异常)。
class GracefulDegradationTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final RechargeReadPort rechargePort = mock(RechargeReadPort.class);
    private final PoolReadPort poolPort = mock(PoolReadPort.class);

    @Test
    void allPublicQueriesEmptyWhenNoCampaign() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());

        assertThat(new LeaderboardQueryService(campaignPort, rechargePort).leaderboard()).isEmpty();
        assertThat(new RecentRechargesQueryService(campaignPort, rechargePort).recentRecharges()).isEmpty();
        assertThat(new PoolQueryService(campaignPort, poolPort).pool()).isEmpty();
    }
}
