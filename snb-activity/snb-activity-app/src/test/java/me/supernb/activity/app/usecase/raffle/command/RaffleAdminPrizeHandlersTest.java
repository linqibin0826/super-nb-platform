package me.supernb.activity.app.usecase.raffle.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.RaffleCampaignNotEditableException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// Prize 增删改三命令:都要先过"期存在且 active"守卫,再委托 RafflePrizePort。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class RaffleAdminPrizeHandlersTest {

    private static RaffleCampaign campaign(String status) {
        return new RaffleCampaign(1, "第三届", Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z"), Instant.parse("2026-07-14T10:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"),
                null, WeightMode.EQUAL, status, null, null, null);
    }

    @Test
    void addRejectsWhenCampaignDrawn() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign("drawn")));
        AddRafflePrizeHandler handler = new AddRafflePrizeHandler(campaignPort, prizePort);
        assertThatThrownBy(() -> handler.handle(new AddRafflePrizeCommand(1, "S", "头奖", "ALIPAY_CODE", "X", 0)))
                .isInstanceOf(RaffleCampaignNotEditableException.class);
    }

    @Test
    void addDelegatesToPrizePortWhenActive() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign("active")));
        when(prizePort.create(1, "S", "头奖", "ALIPAY_CODE", "X", 0)).thenReturn(555L);
        AddRafflePrizeHandler handler = new AddRafflePrizeHandler(campaignPort, prizePort);
        Long id = handler.handle(new AddRafflePrizeCommand(1, "S", "头奖", "ALIPAY_CODE", "X", 0));
        assertThat(id).isEqualTo(555L);
    }

    @Test
    void updateDelegatesWhenActive() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign("active")));
        UpdateRafflePrizeHandler handler = new UpdateRafflePrizeHandler(campaignPort, prizePort);
        handler.handle(new UpdateRafflePrizeCommand(1, 555, "A", "改名", "ALIPAY_CODE", "Y", 1));
        verify(prizePort).update(555, "A", "改名", "ALIPAY_CODE", "Y", 1);
    }

    @Test
    void deleteRejectsWhenCampaignCancelled() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign("cancelled")));
        DeleteRafflePrizeHandler handler = new DeleteRafflePrizeHandler(campaignPort, prizePort);
        assertThatThrownBy(() -> handler.handle(new DeleteRafflePrizeCommand(1, 555)))
                .isInstanceOf(RaffleCampaignNotEditableException.class);
    }

    @Test
    void deleteDelegatesWhenActive() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(campaign("active")));
        DeleteRafflePrizeHandler handler = new DeleteRafflePrizeHandler(campaignPort, prizePort);
        handler.handle(new DeleteRafflePrizeCommand(1, 555));
        verify(prizePort).delete(555);
    }
}
