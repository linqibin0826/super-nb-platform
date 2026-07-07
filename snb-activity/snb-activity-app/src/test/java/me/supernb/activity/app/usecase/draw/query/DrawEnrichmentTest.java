package me.supernb.activity.app.usecase.draw.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.read.CodeStatus;
import me.supernb.activity.domain.model.read.MyDrawView;
import me.supernb.activity.domain.model.read.PublicDraw;
import me.supernb.activity.domain.model.read.RawDraw;
import me.supernb.activity.domain.model.read.RawWinner;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.junit.jupiter.api.Test;

/// recent-draws / my-draws 的 enrich 与降级逻辑。
class DrawEnrichmentTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final RechargeReadPort rechargePort = mock(RechargeReadPort.class);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void recentDrawsSkipsWinnersWithoutEmail() {
        RecentDrawsQueryService useCase = new RecentDrawsQueryService(campaignPort, drawPort, rechargePort);
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.recentRealWinners(1, 500)).thenReturn(List.of(
                new RawWinner(10, new BigDecimal("20")),
                new RawWinner(11, new BigDecimal("50")))); // 11 无邮箱 → 跳过
        when(rechargePort.maskedEmailsByIds(java.util.Set.of(10L, 11L)))
                .thenReturn(Map.of(10L, "ab***@qq.com"));

        List<PublicDraw> draws = useCase.recentDraws();

        assertThat(draws).hasSize(1);
        assertThat(draws.get(0).name()).isEqualTo("ab***@qq.com");
        assertThat(draws.get(0).amount()).isEqualByComparingTo("20");
    }

    @Test
    void recentDrawsEmptyWhenNoCampaign() {
        RecentDrawsQueryService useCase = new RecentDrawsQueryService(campaignPort, drawPort, rechargePort);
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThat(useCase.recentDraws()).isEmpty();
    }

    @Test
    void myDrawsEnrichesCodeStatusAndConsolationHasNone() {
        MyDrawsQueryService useCase = new MyDrawsQueryService(campaignPort, drawPort, rechargePort);
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        Instant t = Instant.parse("2026-07-15T00:00:00Z");
        when(drawPort.myRawDraws(1, 7)).thenReturn(List.of(
                new RawDraw(new BigDecimal("20"), "CODE1", false, t),
                new RawDraw(new BigDecimal("5"), null, true, t))); // 安慰奖无码
        when(rechargePort.codeStatuses(List.of("CODE1")))
                .thenReturn(Map.of("CODE1", new CodeStatus("unused", null)));

        List<MyDrawView> views = useCase.myDraws(7);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).codeStatus()).isEqualTo("unused");
        assertThat(views.get(1).consolation()).isTrue();
        assertThat(views.get(1).codeStatus()).isNull();
    }
}
