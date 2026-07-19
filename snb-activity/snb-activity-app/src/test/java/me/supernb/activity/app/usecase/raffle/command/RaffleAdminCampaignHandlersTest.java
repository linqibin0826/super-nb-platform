package me.supernb.activity.app.usecase.raffle.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.exception.RaffleCampaignNotEditableException;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// Campaign 生命周期三命令:新建校验时间顺序/门槛/账龄;编辑仅限 active;作废任意状态放行。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class RaffleAdminCampaignHandlersTest {

    private static RaffleCampaign campaign(long id, String status) {
        return new RaffleCampaign(id, "第三届", Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z"), Instant.parse("2026-07-14T10:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"),
                null, WeightMode.EQUAL, status, null, null, null);
    }

    // ---- CreateRaffleCampaignHandler ----

    @Test
    void createRejectsWhenEntryCloseNotBeforeDrawAt() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        CreateRaffleCampaignHandler handler = new CreateRaffleCampaignHandler(campaignPort, prizePort);
        Instant t = Instant.parse("2026-07-14T10:00:00Z");
        assertThatThrownBy(() -> handler.handle(new CreateRaffleCampaignCommand("x",
                t.minusSeconds(3600), t, t, GateType.RECHARGE, new BigDecimal("30"),
                Instant.parse("2026-06-01T00:00:00Z"), null, WeightMode.EQUAL, List.of())))
                .isInstanceOf(RaffleAdminValidationException.class)
                .hasMessageContaining("开奖时间");
    }

    @Test
    void createRejectsNonPositiveGateAmount() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        CreateRaffleCampaignHandler handler = new CreateRaffleCampaignHandler(campaignPort, prizePort);
        Instant open = Instant.now();
        assertThatThrownBy(() -> handler.handle(new CreateRaffleCampaignCommand("x", open,
                open.plusSeconds(3600), open.plusSeconds(7200), GateType.RECHARGE, BigDecimal.ZERO,
                Instant.parse("2026-06-01T00:00:00Z"), null, WeightMode.EQUAL, List.of())))
                .isInstanceOf(RaffleAdminValidationException.class)
                .hasMessageContaining("门槛金额");
    }

    @Test
    void createPersistsCampaignThenPrizeSkeletonWithEmptyPayload() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        CreateRaffleCampaignHandler handler = new CreateRaffleCampaignHandler(campaignPort, prizePort);
        Instant open = Instant.now();
        when(campaignPort.create("第四届", open, open.plusSeconds(3600), open.plusSeconds(7200),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"), null,
                WeightMode.EQUAL)).thenReturn(999L);

        Long id = handler.handle(new CreateRaffleCampaignCommand("第四届", open, open.plusSeconds(3600),
                open.plusSeconds(7200), GateType.RECHARGE, new BigDecimal("30"),
                Instant.parse("2026-06-01T00:00:00Z"), null, WeightMode.EQUAL,
                List.of(new CreateRaffleCampaignCommand.PrizeSkeleton("S", "头奖", "ALIPAY_CODE", 0))));

        assertThat(id).isEqualTo(999L);
        verify(prizePort).create(999L, "S", "头奖", "ALIPAY_CODE", "", 0);
    }

    // ---- UpdateRaffleCampaignHandler ----

    @Test
    void updateRejectsWhenCampaignAlreadyDrawn() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign(1, "drawn")));
        UpdateRaffleCampaignHandler handler = new UpdateRaffleCampaignHandler(campaignPort);
        Instant open = Instant.now();
        assertThatThrownBy(() -> handler.handle(new UpdateRaffleCampaignCommand(1, "改名", open,
                open.plusSeconds(3600), open.plusSeconds(7200), GateType.RECHARGE, new BigDecimal("30"),
                Instant.parse("2026-06-01T00:00:00Z"), null, WeightMode.EQUAL)))
                .isInstanceOf(RaffleCampaignNotEditableException.class);
    }

    @Test
    void updateDelegatesWhenActive() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign(1, "active")));
        UpdateRaffleCampaignHandler handler = new UpdateRaffleCampaignHandler(campaignPort);
        Instant open = Instant.now();
        handler.handle(new UpdateRaffleCampaignCommand(1, "改名", open, open.plusSeconds(3600),
                open.plusSeconds(7200), GateType.RECHARGE, new BigDecimal("50"),
                Instant.parse("2026-06-01T00:00:00Z"), 7, WeightMode.EQUAL));
        verify(campaignPort).update(eq(1L), eq("改名"), any(), any(), any(), eq(GateType.RECHARGE),
                eq(new BigDecimal("50")), any(), eq(7), eq(WeightMode.EQUAL));
    }

    // ---- CancelRaffleCampaignHandler ----

    @Test
    void cancelRejectsUnknownCampaign() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.empty());
        CancelRaffleCampaignHandler handler = new CancelRaffleCampaignHandler(campaignPort);
        assertThatThrownBy(() -> handler.handle(new CancelRaffleCampaignCommand(1)))
                .isInstanceOf(RaffleNotFoundException.class);
    }

    @Test
    void cancelWorksEvenWhenAlreadyDrawn() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign(1, "drawn")));
        CancelRaffleCampaignHandler handler = new CancelRaffleCampaignHandler(campaignPort);
        handler.handle(new CancelRaffleCampaignCommand(1));
        verify(campaignPort).cancel(1);
    }
}
