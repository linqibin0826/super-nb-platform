package me.supernb.activity.app.usecase.draw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.draw.command.PerformDrawAllCommand;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.exception.NoDrawsLeftException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import org.junit.jupiter.api.Test;

/// 批量抽奖 Handler:委托 DrawPort.drawAllFor、领域异常按契约直接传播。
class PerformDrawAllHandlerTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final PerformDrawAllHandler handler = new PerformDrawAllHandler(campaignPort, drawPort);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void delegatesToDrawPort() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawAllFor(campaign, 7)).thenReturn(List.of(
                DrawResult.prize(new BigDecimal("20"), "CODE1"),
                DrawResult.prize(new BigDecimal("45"), "CODE2")));

        List<DrawResult> r = handler.handle(new PerformDrawAllCommand(7));

        assertThat(r).hasSize(2);
        assertThat(r.get(1).redeemCode()).isEqualTo("CODE2");
    }

    @Test
    void propagatesNoDrawsLeft() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.drawAllFor(campaign, 7)).thenThrow(new NoDrawsLeftException());
        assertThatThrownBy(() -> handler.handle(new PerformDrawAllCommand(7)))
                .isInstanceOf(NoDrawsLeftException.class);
    }

    @Test
    void noActiveCampaignIsNotActive() {
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new PerformDrawAllCommand(7)))
                .isInstanceOf(CampaignNotActiveException.class);
    }
}
