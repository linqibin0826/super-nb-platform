package me.supernb.activity.adapter.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.app.usecase.draw.command.PerformDrawAllCommand;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.model.read.ReferralInviteEntry;
import me.supernb.activity.domain.model.read.ReferralStats;
import me.supernb.activity.domain.model.read.ReferralRechargeEntry;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 控制器映射 + JSON 契约(standalone MockMvc,happy path)。错误状态码在 boot 装配测试里验。
/// 写端点 mock CommandBus——命令是 record,equals 精确匹配即断言了派发参数。
class ActivityControllerTest {

    private final CommandBus commandBus = mock(CommandBus.class);
    private final DrawStatusQueryService drawStatusQuery = mock(DrawStatusQueryService.class);
    private final LeaderboardQueryService leaderboardQuery = mock(LeaderboardQueryService.class);
    private final RecentRechargesQueryService recentRechargesQuery = mock(RecentRechargesQueryService.class);
    private final PoolQueryService poolQuery = mock(PoolQueryService.class);
    private final RecentDrawsQueryService recentDrawsQuery = mock(RecentDrawsQueryService.class);
    private final MyDrawsQueryService myDrawsQuery = mock(MyDrawsQueryService.class);
    private final ReferralLeaderboardQueryService referralQuery = mock(ReferralLeaderboardQueryService.class);
    private final UsageLeaderboardQueryService usageLeaderboardQuery = mock(UsageLeaderboardQueryService.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ActivityController controller = new ActivityController(
                commandBus, drawStatusQuery, leaderboardQuery, recentRechargesQuery,
                poolQuery, recentDrawsQuery, myDrawsQuery, referralQuery, usageLeaderboardQuery);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void poolIsPublicAndReturnsTiers() throws Exception {
        when(poolQuery.pool()).thenReturn(List.of(new PoolTier(new BigDecimal("10"), 5, 3)));
        mvc.perform(get("/activity/v1/pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(10))
                .andExpect(jsonPath("$[0].total").value(5))
                .andExpect(jsonPath("$[0].available").value(3));
    }

    @Test
    void statusWithValidTokenReturnsEligibility() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(drawStatusQuery.status(7)).thenReturn(new DrawStatus(true, 2));
        mvc.perform(get("/activity/v1/status").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.remaining").value(2));
    }

    @Test
    void drawDispatchesCommandAndReturnsPrizeResult() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(commandBus.handle(new PerformDrawCommand(7))).thenReturn(DrawResult.prize(new BigDecimal("20"), "CODE1"));
        mvc.perform(post("/activity/v1/draw").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redeemCode").value("CODE1"))
                .andExpect(jsonPath("$.consolation").value(false));
    }

    @Test
    void drawAllDispatchesCommandAndReturnsResultArray() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(commandBus.handle(new PerformDrawAllCommand(7))).thenReturn(List.of(
                DrawResult.prize(new BigDecimal("20"), "CODE1"),
                DrawResult.consolation(new BigDecimal("5"))));
        mvc.perform(post("/activity/v1/draw/all").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].redeemCode").value("CODE1"))
                .andExpect(jsonPath("$[0].consolation").value(false))
                .andExpect(jsonPath("$[1].consolation").value(true));
    }

    @Test
    void referralRechargeBoardIsPublicAndReturnsEntries() throws Exception {
        when(referralQuery.rechargeBoard()).thenReturn(List.of(
                new ReferralRechargeEntry("al***@qq.com", new BigDecimal("390"), new BigDecimal("288"))));
        mvc.perform(get("/activity/v1/referral/recharge-board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("al***@qq.com"))
                .andExpect(jsonPath("$[0].total").value(390))
                .andExpect(jsonPath("$[0].capped").value(288));
    }

    @Test
    void referralStatsIsPublicAndReturnsNewcomerTotal() throws Exception {
        when(referralQuery.stats()).thenReturn(new ReferralStats(42));
        mvc.perform(get("/activity/v1/referral/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newcomers").value(42));
    }

    @Test
    void referralInviteBoardIsPublicAndReturnsEntries() throws Exception {
        when(referralQuery.inviteBoard()).thenReturn(List.of(new ReferralInviteEntry("al***@qq.com", 2)));
        mvc.perform(get("/activity/v1/referral/invite-board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("al***@qq.com"))
                .andExpect(jsonPath("$[0].count").value(2));
    }
}
