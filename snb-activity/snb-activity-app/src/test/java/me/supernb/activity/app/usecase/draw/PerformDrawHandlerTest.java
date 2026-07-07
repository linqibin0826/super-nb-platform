package me.supernb.activity.app.usecase.draw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.exception.NoDrawsLeftException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.DrawPort;
import org.junit.jupiter.api.Test;

/// 抽奖 Handler:委托 DrawPort、领域异常按契约直接传播。
class PerformDrawHandlerTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final PerformDrawHandler handler = new PerformDrawHandler(campaignPort, drawPort);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void delegatesToDrawPort() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawFor(campaign, 7)).thenReturn(DrawResult.prize(new BigDecimal("20"), "CODE1"));

        DrawResult r = handler.handle(new PerformDrawCommand(7));

        assertThat(r.consolation()).isFalse();
        assertThat(r.redeemCode()).isEqualTo("CODE1");
    }

    @Test
    void propagatesNoDrawsLeft() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawFor(campaign, 7)).thenThrow(new NoDrawsLeftException());
        assertThatThrownBy(() -> handler.handle(new PerformDrawCommand(7)))
                .isInstanceOf(NoDrawsLeftException.class);
    }

    @Test
    void noActiveCampaignIsNotActive() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new PerformDrawCommand(7)))
                .isInstanceOf(CampaignNotActiveException.class);
    }
}
