package me.supernb.activity.app.usecase.raffle.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleRedeemCodeIssuerPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 生成兑换码:count 越界本地先拦(不等上游 400);正常路径先调签发端口拿码,再批量落库。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class GenerateRaffleRedeemCodesHandlerTest {

    private static RaffleCampaign activeCampaign() {
        return new RaffleCampaign(1, "第三届", Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z"), Instant.parse("2026-07-14T10:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"),
                null, WeightMode.EQUAL, "active", null, null, null);
    }

    @Test
    void rejectsCountOutOfRange() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        RaffleRedeemCodeIssuerPort issuer = mock(RaffleRedeemCodeIssuerPort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        GenerateRaffleRedeemCodesHandler handler =
                new GenerateRaffleRedeemCodesHandler(campaignPort, prizePort, issuer);
        assertThatThrownBy(() -> handler.handle(
                new GenerateRaffleRedeemCodesCommand(1, "C", "体验码", 77, 1, 101, 0)))
                .isInstanceOf(RaffleAdminValidationException.class)
                .hasMessageContaining("1~100");
    }

    @Test
    void issuesThenBatchCreatesPrizes() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        RaffleRedeemCodeIssuerPort issuer = mock(RaffleRedeemCodeIssuerPort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        when(issuer.issue(77, 1, 2)).thenReturn(List.of("code-a", "code-b"));
        when(prizePort.createBatch(1, "C", "体验码", "REDEEM_CODE", List.of("code-a", "code-b"), 5))
                .thenReturn(List.of(101L, 102L));

        GenerateRaffleRedeemCodesHandler handler =
                new GenerateRaffleRedeemCodesHandler(campaignPort, prizePort, issuer);
        List<Long> ids = handler.handle(new GenerateRaffleRedeemCodesCommand(1, "C", "体验码", 77, 1, 2, 5));

        assertThat(ids).containsExactly(101L, 102L);
        verify(issuer).issue(77, 1, 2);
    }
}
