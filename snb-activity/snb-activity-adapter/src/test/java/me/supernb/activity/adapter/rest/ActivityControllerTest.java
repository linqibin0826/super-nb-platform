package me.supernb.activity.adapter.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import me.supernb.activity.app.usecase.achievement.command.MarkAchievementsSeenCommand;
import me.supernb.activity.app.usecase.achievement.query.AchievementWallQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinRewardQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinStatusQueryService;
import me.supernb.activity.domain.model.achievement.AchievementCategoryView;
import me.supernb.activity.domain.model.achievement.AchievementItemView;
import me.supernb.activity.domain.model.achievement.AchievementSummaryView;
import me.supernb.activity.domain.model.achievement.AchievementWallView;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.app.usecase.draw.command.PerformDrawAllCommand;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.gate.GateDrawResult;
import me.supernb.activity.app.usecase.gate.command.PerformGateDrawCommand;
import me.supernb.activity.app.usecase.raffle.RaffleQueryService;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.registry.query.RegistryStatusQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.model.read.ReferralInviteEntry;
import me.supernb.activity.domain.model.read.ReferralStats;
import me.supernb.activity.domain.model.read.ReferralRechargeEntry;
import me.supernb.activity.domain.model.read.registry.RegistryEntryStatus;
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
    private final RegistryStatusQueryService registryStatusQuery = mock(RegistryStatusQueryService.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);
    private final AchievementWallQueryService achievementWallQuery = mock(AchievementWallQueryService.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ActivityController controller = new ActivityController(
                commandBus, drawStatusQuery, leaderboardQuery, recentRechargesQuery,
                poolQuery, recentDrawsQuery, myDrawsQuery, referralQuery, usageLeaderboardQuery,
                mock(RaffleQueryService.class), registryStatusQuery,
                mock(CheckinStatusQueryService.class), mock(CheckinRewardQueryService.class),
                achievementWallQuery);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void gateDrawReturnsOwnCodeOnlyWithWhitelistedFields() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(commandBus.handle(any(PerformGateDrawCommand.class)))
                .thenReturn(new GateDrawResult(true, true, new BigDecimal("6"), "TK-6",
                        Instant.parse("2026-07-12T12:00:00Z")));
        mvc.perform(post("/activity/v1/gate/draw").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.win").value(true))
                .andExpect(jsonPath("$.amount").value(6))
                .andExpect(jsonPath("$.code").value("TK-6"))
                .andExpect(jsonPath("$.*", hasSize(5))); // 白名单五字段,多一个都不行
    }

    @Test
    void gateDrawIneligibleHidesEverything() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(commandBus.handle(any(PerformGateDrawCommand.class))).thenReturn(GateDrawResult.ineligible());
        mvc.perform(post("/activity/v1/gate/draw").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.win").value(false))
                .andExpect(jsonPath("$.code").isEmpty());
    }

    @Test
    void registryStatusIsPublicAndExposesOnlyWhitelistedFields() throws Exception {
        when(registryStatusQuery.status()).thenReturn(List.of(
                new RegistryEntryStatus("qq-referral", "qq-referral", "running",
                        Instant.parse("2026-07-09T16:00:00Z"), Instant.parse("2026-07-16T16:00:00Z")),
                new RegistryEntryStatus("leaderboard", "evergreen", "running", null, null)));
        mvc.perform(get("/activity/v1/registry-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaigns[0].id").value("qq-referral"))
                .andExpect(jsonPath("$.campaigns[0].status").value("running"))
                .andExpect(jsonPath("$.campaigns[0].startsAt").value("2026-07-09T16:00:00Z"))
                .andExpect(jsonPath("$.campaigns[1].kind").value("evergreen"))
                .andExpect(jsonPath("$.campaigns[0].*", hasSize(5))); // 白名单五字段,多一个都不行
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

    @Test
    void achievementsEndpointReturnsWallShapedResponse() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(42, "user", "active")));
        AchievementWallView view = new AchievementWallView(
                new AchievementSummaryView(1, 1, 5),
                List.of(new AchievementCategoryView("入职档案", false,
                        List.of(new AchievementItemView("checkin_first", "开机自检", "首次签到", 1, 5, true,
                                "active", false, "每天上班第一件事:证明我还活着。", null, null, null, null, null)),
                        List.of())),
                List.of(),
                List.of());
        when(achievementWallQuery.wall(42)).thenReturn(view);

        mvc.perform(get("/activity/v1/checkin/achievements").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.unlockedCount").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("入职档案"))
                .andExpect(jsonPath("$.categories[0].items[0].condition").value("首次签到"))
                .andExpect(jsonPath("$.categories[0].items[0].tier").value(1));
    }

    @Test
    void seenEndpointDelegatesToCommandBusAndReturnsAcknowledgedCount() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(42, "user", "active")));
        when(commandBus.handle(any(MarkAchievementsSeenCommand.class))).thenReturn(1);

        mvc.perform(post("/activity/v1/checkin/achievements/seen")
                        .header("Authorization", "Bearer T")
                        .contentType("application/json")
                        .content("{\"codes\":[\"api_calls_2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledged").value(1));
    }
}
