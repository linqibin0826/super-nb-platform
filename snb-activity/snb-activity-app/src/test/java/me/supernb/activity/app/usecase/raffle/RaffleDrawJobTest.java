package me.supernb.activity.app.usecase.raffle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleDrawPort;
import org.junit.jupiter.api.Test;

/// 开奖任务:逐期隔离,一期失败不拖垮同批其余期。
class RaffleDrawJobTest {

    private static RaffleCampaign due(long id) {
        Instant past = Instant.now().minusSeconds(60);
        return new RaffleCampaign(id, "期" + id, past.minusSeconds(600), past, past, GateType.RECHARGE,
                new BigDecimal("100"), past.minusSeconds(6000), null, WeightMode.EQUAL, "active",
                null, null, null);
    }

    @Test
    void oneCampaignFailureDoesNotBlockOthers() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RaffleDrawPort drawPort = mock(RaffleDrawPort.class);
        when(campaignPort.dueForDraw(any())).thenReturn(List.of(due(1), due(2)));
        doThrow(new RuntimeException("boom")).when(drawPort).drawCampaign(eq(1L), any());

        new RaffleDrawJob(campaignPort, drawPort, new Random(7)).drawDue();

        verify(drawPort).drawCampaign(eq(2L), any()); // 第 2 期仍被开奖
    }
}
