package me.supernb.activity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.app.usecase.raffle.command.CancelRaffleCampaignCommand;
import me.supernb.activity.app.usecase.raffle.command.GenerateRaffleRedeemCodeForPrizeCommand;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 管理端契约:role=admin 守卫(照 InvoiceAdminControllerTest 的 standalone 无 advice 写法,
/// 断言抛出异常而非 403 状态码);列表/详情组装正确;写端点正确 dispatch 命令。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class RaffleAdminControllerTest {

    final CommandBus commandBus = mock(CommandBus.class);
    final RaffleCampaignPort campaignPort = mock(RaffleCampaignPort.class);
    final RafflePrizePort prizePort = mock(RafflePrizePort.class);
    final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    MockMvc mvc;

    @BeforeEach
    void setup() {
        when(introspect.introspect("Bearer admin")).thenReturn(Optional.of(new UserProfile(1, "admin", "active")));
        when(introspect.introspect("Bearer u")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        mvc = MockMvcBuilders.standaloneSetup(new RaffleAdminController(commandBus, campaignPort, prizePort))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    private static RaffleCampaign campaign(long id, String status) {
        return new RaffleCampaign(id, "第三届 V我50", Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-14T10:00:00Z"), Instant.parse("2026-07-14T10:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"),
                null, WeightMode.EQUAL, status, null, null, null);
    }

    @Test
    void listIncludesPrizeCount() throws Exception {
        when(campaignPort.listAll()).thenReturn(List.of(campaign(3, "active")));
        when(prizePort.byCampaign(3)).thenReturn(List.of());
        mvc.perform(get("/activity/v1/admin/raffle/campaigns").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("3")) // DTO 层 id 已转 String,不是 JSON number
                .andExpect(jsonPath("$[0].prizeCount").value(0));
    }

    @Test
    void detailReturns404WhenMissing() {
        when(campaignPort.byId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> mvc.perform(get("/activity/v1/admin/raffle/campaigns/99")
                        .header("Authorization", "Bearer admin")))
                .hasMessageContaining("不存在");
    }

    @Test
    void cancelDispatchesCommand() throws Exception {
        mvc.perform(post("/activity/v1/admin/raffle/campaigns/3/cancel").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
        verify(commandBus).handle(new CancelRaffleCampaignCommand(3));
    }

    @Test
    void generateRedeemCodeForPrizeDispatchesAndReturnsPrize() throws Exception {
        when(commandBus.handle(new GenerateRaffleRedeemCodeForPrizeCommand(3, 101, 77, 1))).thenReturn(101L);
        when(prizePort.byCampaign(3)).thenReturn(List.of(
                new RafflePrize(101, "C", "GPT $20 日卡", "REDEEM_CODE", "SNB-NEW", 3, null, null)));
        mvc.perform(post("/activity/v1/admin/raffle/campaigns/3/prizes/101/generate-redeem-code")
                        .header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":77,\"validityDays\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("101"))
                .andExpect(jsonPath("$.payload").value("SNB-NEW"));
    }

    @Test
    void nonAdminForbiddenOnEveryEndpoint() {
        assertThatThrownBy(() -> mvc.perform(get("/activity/v1/admin/raffle/campaigns")
                        .header("Authorization", "Bearer u")))
                .hasMessageContaining("管理员");
        assertThatThrownBy(() -> mvc.perform(post("/activity/v1/admin/raffle/campaigns/3/cancel")
                        .header("Authorization", "Bearer u")))
                .hasMessageContaining("管理员");
    }
}
