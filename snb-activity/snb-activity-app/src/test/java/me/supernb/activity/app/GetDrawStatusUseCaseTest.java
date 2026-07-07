package me.supernb.activity.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import me.supernb.activity.domain.Campaign;
import me.supernb.activity.domain.CampaignNotActiveException;
import org.junit.jupiter.api.Test;

class GetDrawStatusUseCaseTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final RechargeQueryPort rechargePort = mock(RechargeQueryPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final GetDrawStatusUseCase useCase = new GetDrawStatusUseCase(campaignPort, rechargePort, drawPort);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void computesEligibilityAndRemaining() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(rechargePort.totalRecharge(7, campaign.startsAt(), campaign.endsAt())).thenReturn(new BigDecimal("250"));
        when(drawPort.countDraws(1, 7)).thenReturn(1);

        ActivityDto.DrawStatus s = useCase.status(7);

        assertThat(s.eligible()).isTrue();
        assertThat(s.remaining()).isEqualTo(1); // floor(250/100)=2 - used 1
    }

    @Test
    void notEligibleBelowThreshold() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(rechargePort.totalRecharge(7, campaign.startsAt(), campaign.endsAt())).thenReturn(new BigDecimal("80"));
        when(drawPort.countDraws(1, 7)).thenReturn(0);

        ActivityDto.DrawStatus s = useCase.status(7);

        assertThat(s.eligible()).isFalse();
        assertThat(s.remaining()).isZero();
    }

    @Test
    void throwsWhenNoActiveCampaign() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.status(7)).isInstanceOf(CampaignNotActiveException.class);
    }
}
