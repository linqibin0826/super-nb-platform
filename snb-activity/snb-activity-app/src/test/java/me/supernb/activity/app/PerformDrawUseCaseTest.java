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
import me.supernb.activity.domain.DrawResult;
import me.supernb.activity.domain.NoDrawsLeftException;
import org.junit.jupiter.api.Test;

class PerformDrawUseCaseTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final PerformDrawUseCase useCase = new PerformDrawUseCase(campaignPort, drawPort);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void delegatesToDrawPort() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawFor(campaign, 7)).thenReturn(DrawResult.prize(new BigDecimal("20"), "CODE1"));

        DrawResult r = useCase.draw(7);

        assertThat(r.consolation()).isFalse();
        assertThat(r.redeemCode()).isEqualTo("CODE1");
    }

    @Test
    void propagatesNoDrawsLeft() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawFor(campaign, 7)).thenThrow(new NoDrawsLeftException());
        assertThatThrownBy(() -> useCase.draw(7)).isInstanceOf(NoDrawsLeftException.class);
    }

    @Test
    void throwsWhenNoActiveCampaign() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.draw(7)).isInstanceOf(CampaignNotActiveException.class);
    }
}
