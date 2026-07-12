package me.supernb.activity.app.usecase.raffle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.raffle.command.RegisterRaffleCommand;
import me.supernb.activity.app.usecase.raffle.command.RegisterRaffleHandler;
import me.supernb.activity.domain.exception.RaffleNotEligibleException;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.exception.RaffleNotOpenException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleEntryPort;
import me.supernb.activity.domain.port.read.RaffleGateReadPort;
import org.junit.jupiter.api.Test;

/// 报名 Handler:窗口/账龄/门槛三道闸,全过才委托 enter。
class RegisterRaffleHandlerTest {

    private final RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
    private final RaffleEntryPort entryPort = mock(RaffleEntryPort.class);
    private final RaffleGateReadPort gatePort = mock(RaffleGateReadPort.class);
    private final RegisterRaffleHandler handler =
            new RegisterRaffleHandler(campaignPort, entryPort, gatePort);

    private static RaffleCampaign campaign(Instant open, Instant close, Integer minAgeDays) {
        return new RaffleCampaign(1, "第一届发布会", open, close, close, GateType.RECHARGE,
                new BigDecimal("100"), Instant.parse("2026-07-01T00:00:00Z"), minAgeDays,
                WeightMode.EQUAL, "active", null, null, null);
    }

    @Test
    void unknownCampaignRejected() {
        when(campaignPort.byId(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new RegisterRaffleCommand(1, 42, null, null)))
                .isInstanceOf(RaffleNotFoundException.class);
    }

    @Test
    void closedWindowRejected() {
        Instant past = Instant.now().minusSeconds(3600);
        when(campaignPort.byId(1)).thenReturn(Optional.of(campaign(past.minusSeconds(60), past, null)));
        assertThatThrownBy(() -> handler.handle(new RegisterRaffleCommand(1, 42, null, null)))
                .isInstanceOf(RaffleNotOpenException.class);
    }

    @Test
    void belowGateRejectedWithShortfallMessage() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(
                campaign(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), null)));
        when(gatePort.gateValue(eq(42L), eq(GateType.RECHARGE), any(), any()))
                .thenReturn(new BigDecimal("61.50"));
        assertThatThrownBy(() -> handler.handle(new RegisterRaffleCommand(1, 42, null, null)))
                .isInstanceOf(RaffleNotEligibleException.class)
                .hasMessageContaining("38.5"); // 100 - 61.50 去尾零
    }

    @Test
    void youngAccountRejectedWhenAgeGateConfigured() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(
                campaign(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), 7)));
        when(gatePort.registeredAts(any())).thenReturn(Map.of(42L, Instant.now().minusSeconds(3600)));
        assertThatThrownBy(() -> handler.handle(new RegisterRaffleCommand(1, 42, null, null)))
                .isInstanceOf(RaffleNotEligibleException.class)
                .hasMessageContaining("7 天");
    }

    @Test
    void eligibleDelegatesToEnterWithComputedValue() {
        when(campaignPort.byId(1)).thenReturn(Optional.of(
                campaign(Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600), null)));
        when(gatePort.gateValue(eq(42L), eq(GateType.RECHARGE), any(), any()))
                .thenReturn(new BigDecimal("130"));
        when(entryPort.enter(eq(1L), eq(42L), eq(new BigDecimal("130")), eq("1.2.3.4"), eq("UA")))
                .thenReturn(new RaffleEntryTicket(37, false));
        RaffleEntryTicket t = handler.handle(new RegisterRaffleCommand(1, 42, "1.2.3.4", "UA"));
        assertThat(t.entryNo()).isEqualTo(37);
        assertThat(t.already()).isFalse();
    }
}
