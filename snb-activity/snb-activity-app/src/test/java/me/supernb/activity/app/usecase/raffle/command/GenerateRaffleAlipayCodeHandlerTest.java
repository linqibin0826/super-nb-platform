package me.supernb.activity.app.usecase.raffle.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 生成口令:prizeId 为空走新建,非空走回填(不重复传 tier/displayName/sortOrder)。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class GenerateRaffleAlipayCodeHandlerTest {

    private static RaffleCampaign activeCampaign() {
        return new RaffleCampaign(1, "第三届", Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z"), Instant.parse("2026-07-14T10:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"),
                null, WeightMode.EQUAL, "active", null, null, null);
    }

    @Test
    void createsNewPrizeWhenPrizeIdAbsent() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        // 一旦某个参数用了 matcher,该次 stub 的全部参数都必须是 matcher(Mockito 硬性规则),
        // 所以字面量也要包一层 eq(...),不能混用裸值 + anyString()。
        when(prizePort.create(eq(1L), eq("S"), eq("头奖"), eq("ALIPAY_CODE"), anyString(), eq(0)))
                .thenReturn(777L);

        GenerateRaffleAlipayCodeHandler handler = new GenerateRaffleAlipayCodeHandler(campaignPort, prizePort);
        Long id = handler.handle(new GenerateRaffleAlipayCodeCommand(1, null, "S", "头奖", 0));

        assertThat(id).isEqualTo(777L);
    }

    @Test
    void fillsExistingPrizeWhenPrizeIdPresent() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));

        GenerateRaffleAlipayCodeHandler handler = new GenerateRaffleAlipayCodeHandler(campaignPort, prizePort);
        Long id = handler.handle(new GenerateRaffleAlipayCodeCommand(1, 555L, "忽略", "忽略", 9));

        assertThat(id).isEqualTo(555L);
        verify(prizePort).updatePayload(eq(555L), anyString());
    }

    @Test
    void generatedPassphraseHasSuperNBPrefixAndSixSuffixChars() {
        RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
        RafflePrizePort prizePort = mock(RafflePrizePort.class);
        when(campaignPort.byId(1L)).thenReturn(Optional.of(activeCampaign()));
        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        GenerateRaffleAlipayCodeHandler handler = new GenerateRaffleAlipayCodeHandler(campaignPort, prizePort);
        handler.handle(new GenerateRaffleAlipayCodeCommand(1, 555L, "x", "x", 0));

        verify(prizePort).updatePayload(eq(555L), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).hasSize(13).startsWith("SuperNB"); // "SuperNB" 7 + 6 位随机
    }
}
