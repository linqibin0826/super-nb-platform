package me.supernb.activity.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.domain.Campaign;
import org.junit.jupiter.api.Test;

/// recent-draws / my-draws 的 enrich 与降级逻辑。
class DrawEnrichmentUseCaseTest {

    private final CampaignPort campaignPort = mock(CampaignPort.class);
    private final DrawPort drawPort = mock(DrawPort.class);
    private final RechargeQueryPort rechargePort = mock(RechargeQueryPort.class);

    private final Campaign campaign = new Campaign(
            1, "c", Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"),
            "active", new BigDecimal("5"));

    @Test
    void recentDrawsSkipsWinnersWithoutEmail() {
        GetRecentDrawsUseCase useCase = new GetRecentDrawsUseCase(campaignPort, drawPort, rechargePort);
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        when(drawPort.recentRealWinners(1, 500)).thenReturn(List.of(
                new ActivityDto.RawWinner(10, new BigDecimal("20")),
                new ActivityDto.RawWinner(11, new BigDecimal("50")))); // 11 无邮箱 → 跳过
        when(rechargePort.maskedEmailsByIds(java.util.Set.of(10L, 11L)))
                .thenReturn(Map.of(10L, "ab***@qq.com"));

        List<ActivityDto.PublicDraw> draws = useCase.recentDraws();

        assertThat(draws).hasSize(1);
        assertThat(draws.get(0).name()).isEqualTo("ab***@qq.com");
        assertThat(draws.get(0).amount()).isEqualByComparingTo("20");
    }

    @Test
    void recentDrawsEmptyWhenNoCampaign() {
        GetRecentDrawsUseCase useCase = new GetRecentDrawsUseCase(campaignPort, drawPort, rechargePort);
        when(campaignPort.activeCampaign()).thenReturn(Optional.empty());
        assertThat(useCase.recentDraws()).isEmpty();
    }

    @Test
    void myDrawsEnrichesCodeStatusAndConsolationHasNone() {
        GetMyDrawsUseCase useCase = new GetMyDrawsUseCase(campaignPort, drawPort, rechargePort);
        when(campaignPort.activeCampaign()).thenReturn(Optional.of(campaign));
        Instant t = Instant.parse("2026-07-15T00:00:00Z");
        when(drawPort.myRawDraws(1, 7)).thenReturn(List.of(
                new ActivityDto.RawDraw(new BigDecimal("20"), "CODE1", false, t),
                new ActivityDto.RawDraw(new BigDecimal("5"), null, true, t))); // 安慰奖无码
        when(rechargePort.codeStatuses(List.of("CODE1")))
                .thenReturn(Map.of("CODE1", new ActivityDto.CodeStatus("unused", null)));

        List<ActivityDto.MyDrawView> views = useCase.myDraws(7);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).codeStatus()).isEqualTo("unused");
        assertThat(views.get(1).consolation()).isTrue();
        assertThat(views.get(1).codeStatus()).isNull();
    }
}
