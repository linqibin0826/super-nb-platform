package me.supernb.activity.app.usecase.draw.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawExclusionPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.junit.jupiter.api.Test;

class DrawStatusQueryServiceTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final RechargeReadPort rechargePort = mock(RechargeReadPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final DrawExclusionPort exclusionPort = mock(DrawExclusionPort.class);
    private final DrawStatusQueryService useCase =
            new DrawStatusQueryService(campaignPort, rechargePort, drawPort, exclusionPort);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void computesEligibilityAndRemaining() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(rechargePort.totalRecharge(7, campaign.startsAt(), campaign.endsAt())).thenReturn(new BigDecimal("250"));
        when(drawPort.countDraws(1, 7)).thenReturn(1);

        DrawStatus s = useCase.status(7);

        assertThat(s.eligible()).isTrue();
        assertThat(s.remaining()).isEqualTo(1); // floor(250/100)=2 - used 1
        assertThat(s.excluded()).isFalse();
    }

    @Test
    void notEligibleBelowThreshold() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(rechargePort.totalRecharge(7, campaign.startsAt(), campaign.endsAt())).thenReturn(new BigDecimal("40"));
        when(drawPort.countDraws(1, 7)).thenReturn(0);

        DrawStatus s = useCase.status(7);

        assertThat(s.eligible()).isFalse();
        assertThat(s.remaining()).isZero();
        assertThat(s.excluded()).isFalse();
    }

    /// 中转接入(被排除分组的授权用户)充够了也不算:eligible=false、remaining=0、excluded=true,
    /// 前端据 excluded 弹「这是给散户抽的」而不是「充值满 ¥100 解锁」。
    @Test
    void excludedUserIsNeverEligibleEvenWhenRechargedEnough() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(exclusionPort.isExcluded(7)).thenReturn(true);
        when(rechargePort.totalRecharge(7, campaign.startsAt(), campaign.endsAt())).thenReturn(new BigDecimal("1100"));
        when(drawPort.countDraws(1, 7)).thenReturn(0);

        DrawStatus s = useCase.status(7);

        assertThat(s.excluded()).isTrue();
        assertThat(s.eligible()).isFalse();
        assertThat(s.remaining()).isZero();
    }

    @Test
    void throwsWhenNoActiveCampaign() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.status(7)).isInstanceOf(CampaignNotActiveException.class);
    }
}
