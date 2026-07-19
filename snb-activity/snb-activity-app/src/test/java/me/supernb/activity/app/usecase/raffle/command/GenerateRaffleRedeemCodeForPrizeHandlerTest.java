package me.supernb.activity.app.usecase.raffle.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.exception.RafflePrizeNotFoundException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.activity.domain.port.raffle.RaffleRedeemCodeIssuerPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 逐行生成兑换码(克隆骨架点亮主路径):对空壳兑换码行发 1 张码就地回填;
/// 非兑换码行、已有码值的行一律拒绝——防真码被覆盖成孤儿码(sub2api 侧码活着但页面失联)。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class GenerateRaffleRedeemCodeForPrizeHandlerTest {

    private final RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
    private final RafflePrizePort prizePort = mock(RafflePrizePort.class);
    private final RaffleRedeemCodeIssuerPort issuer = mock(RaffleRedeemCodeIssuerPort.class);
    private final GenerateRaffleRedeemCodeForPrizeHandler handler =
            new GenerateRaffleRedeemCodeForPrizeHandler(campaignPort, prizePort, issuer);

    private static RaffleCampaign activeCampaign() {
        return new RaffleCampaign(1, "第三届", Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z"), Instant.parse("2026-07-14T10:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"),
                null, WeightMode.EQUAL, "active", null, null, null);
    }

    @Test
    void issuesSingleCodeAndFillsPrize() {
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        when(prizePort.byCampaign(1)).thenReturn(List.of(
                new RafflePrize(101, "C", "GPT $20 日卡", "REDEEM_CODE", "", 3, null, null)));
        when(issuer.issue(77, 1, 1)).thenReturn(List.of("code-a"));

        long prizeId = handler.handle(new GenerateRaffleRedeemCodeForPrizeCommand(1, 101, 77, 1));

        assertThat(prizeId).isEqualTo(101L);
        verify(prizePort).updatePayload(101, "code-a");
    }

    @Test
    void rejectsNonRedeemKindPrize() {
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        when(prizePort.byCampaign(1)).thenReturn(List.of(
                new RafflePrize(101, "B", "瑞幸 9.9", "ALIPAY_CODE", "", 2, null, null)));
        assertThatThrownBy(() -> handler.handle(new GenerateRaffleRedeemCodeForPrizeCommand(1, 101, 77, 1)))
                .isInstanceOf(RaffleAdminValidationException.class);
        verify(issuer, never()).issue(anyLong(), anyInt(), anyInt());
    }

    @Test
    void rejectsPrizeAlreadyFilled() {
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        when(prizePort.byCampaign(1)).thenReturn(List.of(
                new RafflePrize(101, "C", "GPT $20 日卡", "REDEEM_CODE", "SNB-OLD", 3, null, null)));
        assertThatThrownBy(() -> handler.handle(new GenerateRaffleRedeemCodeForPrizeCommand(1, 101, 77, 1)))
                .isInstanceOf(RaffleAdminValidationException.class);
        verify(issuer, never()).issue(anyLong(), anyInt(), anyInt());
    }

    @Test
    void rejectsInvalidGroupIdBeforePrizeLookup() {
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        assertThatThrownBy(() -> handler.handle(new GenerateRaffleRedeemCodeForPrizeCommand(1, 101, 0, 1)))
                .isInstanceOf(RaffleAdminValidationException.class);
    }

    @Test
    void propagatesPrizeNotFound() {
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        when(prizePort.byCampaign(1)).thenReturn(List.of());
        assertThatThrownBy(() -> handler.handle(new GenerateRaffleRedeemCodeForPrizeCommand(1, 999, 77, 1)))
                .isInstanceOf(RafflePrizeNotFoundException.class);
    }
}
