package me.supernb.activity.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.achievement.query.AchievementWallQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinRewardQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.raffle.RaffleQueryService;
import me.supernb.activity.app.usecase.raffle.command.RegisterRaffleCommand;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.registry.query.RegistryStatusQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;
import me.supernb.activity.domain.model.read.raffle.MyRaffleView;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;
import me.supernb.activity.domain.model.read.raffle.RaffleCurrentView;
import me.supernb.activity.domain.model.read.raffle.RaffleResultView;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// raffle 端点契约:公开响应无 payload 字段、me 只对中奖本人吐 payload、
/// 雪花 id 字符串、报名转发 ip/ua、非法 id 400。
class RaffleEndpointTest {

    private final RaffleQueryService raffleQuery = mock(RaffleQueryService.class);
    private final CommandBus commandBus = mock(CommandBus.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ActivityController controller = new ActivityController(
                commandBus, mock(DrawStatusQueryService.class), mock(LeaderboardQueryService.class),
                mock(RecentRechargesQueryService.class), mock(PoolQueryService.class),
                mock(RecentDrawsQueryService.class), mock(MyDrawsQueryService.class),
                mock(ReferralLeaderboardQueryService.class), mock(UsageLeaderboardQueryService.class),
                raffleQuery, mock(RegistryStatusQueryService.class),
                mock(CheckinStatusQueryService.class), mock(CheckinRewardQueryService.class),
                mock(AchievementWallQueryService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(42, "user", "active")));
    }

    private static RaffleCurrentView currentView() {
        return new RaffleCurrentView(1, "第一届发布会",
                Instant.parse("2026-07-10T00:00:00Z"), Instant.parse("2026-07-13T02:00:00Z"),
                Instant.parse("2026-07-13T02:30:00Z"), "RECHARGE", new BigDecimal("100"),
                Instant.parse("2026-07-10T00:00:00Z"), "EQUAL", "active", 128,
                List.of(new RaffleCurrentView.Entrant(37, "12***67@qq.com")),
                List.of(new RaffleCurrentView.PrizeLine("S", "疯狂星期四专项(V我50)", "ALIPAY_CODE", 1)));
    }

    @Test
    void currentIsPublicWithServerNowStringIdAndNoPayload() throws Exception {
        when(raffleQuery.current()).thenReturn(Optional.of(currentView()));
        mvc.perform(get("/activity/v1/raffle/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverNow").exists())
                .andExpect(jsonPath("$.campaign.id").value("1"))               // 雪花 id 字符串
                .andExpect(jsonPath("$.campaign.gateType").value("recharge"))  // 枚举值小写
                .andExpect(jsonPath("$.campaign.prizes[0].count").value(1))
                .andExpect(jsonPath("$..payload").doesNotExist());             // 公开契约红线
    }

    @Test
    void currentWithoutCampaignYieldsNullCampaign() throws Exception {
        when(raffleQuery.current()).thenReturn(Optional.empty());
        mvc.perform(get("/activity/v1/raffle/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverNow").exists());
    }

    @Test
    void resultIsPublicAndCarriesNoPayload() throws Exception {
        when(raffleQuery.result(1)).thenReturn(new RaffleResultView(1, "第一届发布会",
                Instant.parse("2026-07-13T02:30:00Z"), 128, 3,
                List.of(new RaffleResultView.Winner(37, "老王", "S", "疯狂星期四专项(V我50)"))));
        mvc.perform(get("/activity/v1/raffle/1/result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value("1"))
                .andExpect(jsonPath("$.winners[0].entryNo").value(37))
                .andExpect(jsonPath("$.winners[0].displayName").value("老王"))
                .andExpect(jsonPath("$..payload").doesNotExist());
    }

    @Test
    void meRevealsPayloadOnlyHere() throws Exception {
        when(raffleQuery.me(1, 42)).thenReturn(new MyRaffleView(true, 37,
                new BigDecimal("150"), new BigDecimal("100"), true,
                new MyRaffleView.MyPrize("S", "疯狂星期四专项(V我50)", "ALIPAY_CODE", "FAKE-KFC-50")));
        mvc.perform(get("/activity/v1/raffle/me").param("campaignId", "1")
                        .header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryNo").value(37))
                .andExpect(jsonPath("$.myPrize.payload").value("FAKE-KFC-50")); // 唯一放行位置
    }

    @Test
    void enterDelegatesCommandWithIpAndUa() throws Exception {
        when(commandBus.handle(any(RegisterRaffleCommand.class)))
                .thenReturn(new RaffleEntryTicket(37, false));
        mvc.perform(post("/activity/v1/raffle/enter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campaignId\":\"1\"}")
                        .header("Authorization", "Bearer T")
                        // 伪造首值 9.9.9.9 + Caddy 亲验真实对端 10.0.0.1(末值)
                        .header("X-Forwarded-For", "9.9.9.9, 10.0.0.1")
                        .header("User-Agent", "TestUA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryNo").value(37))
                .andExpect(jsonPath("$.already").value(false));
        ArgumentCaptor<RegisterRaffleCommand> captor = ArgumentCaptor.forClass(RegisterRaffleCommand.class);
        verify(commandBus).handle(captor.capture());
        assertThat(captor.getValue().campaignId()).isEqualTo(1L);
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().clientIp()).isEqualTo("10.0.0.1"); // XFF 取末值(真实对端,首值可伪造)
        assertThat(captor.getValue().userAgent()).isEqualTo("TestUA");
    }

    @Test
    void invalidCampaignIdRejected400() throws Exception {
        mvc.perform(get("/activity/v1/raffle/abc/result"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void winsIsPublicAndCarriesNoPayloadNorUserId() throws Exception {
        when(raffleQuery.personWins(1, 37)).thenReturn(new PersonWinsView("12***67@qq.com", List.of(
                new PersonWinsView.Win(1, "第一届发布会", Instant.parse("2026-07-13T02:30:00Z"),
                        "S", "疯狂星期四专项(V我50)"))));
        mvc.perform(get("/activity/v1/raffle/wins").param("campaignId", "1").param("entryNo", "37"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("12***67@qq.com"))
                .andExpect(jsonPath("$.wins[0].campaignId").value("1"))
                .andExpect(jsonPath("$.wins[0].tier").value("S"))
                .andExpect(jsonPath("$..payload").doesNotExist())
                .andExpect(jsonPath("$..userId").doesNotExist());
    }

    @Test
    void winsRejectsMalformedEntryNo() throws Exception {
        mvc.perform(get("/activity/v1/raffle/wins").param("campaignId", "1").param("entryNo", "abc"))
                .andExpect(status().isBadRequest());
    }
}
