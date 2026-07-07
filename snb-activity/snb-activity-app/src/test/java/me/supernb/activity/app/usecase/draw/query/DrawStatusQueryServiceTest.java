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
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.junit.jupiter.api.Test;

class DrawStatusQueryServiceTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final RechargeReadPort rechargePort = mock(RechargeReadPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final DrawStatusQueryService useCase = new DrawStatusQueryService(campaignPort, rechargePort, drawPort);

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
    }

    @Test
    void notEligibleBelowThreshold() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(rechargePort.totalRecharge(7, campaign.startsAt(), campaign.endsAt())).thenReturn(new BigDecimal("80"));
        when(drawPort.countDraws(1, 7)).thenReturn(0);

        DrawStatus s = useCase.status(7);

        assertThat(s.eligible()).isFalse();
        assertThat(s.remaining()).isZero();
    }

    @Test
    void throwsWhenNoActiveCampaign() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.status(7)).isInstanceOf(CampaignNotActiveException.class);
    }
}
