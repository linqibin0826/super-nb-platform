package me.supernb.activity.adapter.rest;

import static org.hamcrest.Matchers.nullValue;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.checkin.command.CheckInCommand;
import me.supernb.activity.app.usecase.achievement.query.AchievementWallQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinRewardQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.raffle.RaffleQueryService;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.registry.query.RegistryStatusQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.checkin.CheckInResult;
import me.supernb.activity.domain.model.checkin.CheckinMilestoneView;
import me.supernb.activity.domain.model.checkin.CheckinRewardSummary;
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.model.checkin.CheckinSupplyTierView;
import me.supernb.activity.domain.model.checkin.CheckinSupplyView;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 签到三端点契约(standalone MockMvc,happy path)。401/403/409 状态码映射不在此断言——
/// 该装配不含 commons 的全局错误处理器(`UnauthorizedException`/`CheckinTooYoungException`/
/// `CheckinAlreadyDoneException` 都是走 `StandardErrorTrait` 的 `DomainException`,只有真实
/// Spring 上下文里的全局处理器才转译成 HTTP 状态码——standalone 装配下未登录会话直接把
/// `UnauthorizedException` 原样抛出,断言 `status().isUnauthorized()` 反而会让测试本身炸掉;
/// 家族里既有 `ActivityControllerTest`/`RaffleEndpointTest`/`UsageLeaderboardEndpointTest`
/// 同理都只测已登录路径),留给 Task 16 的 `ActivityWiringTest`(真实 Spring 上下文)验证。
/// POST /checkin 成功响应体是与 GET /checkin/status 完全同形的完整状态快照(2026-07-14 控制器
/// 裁决,覆盖三字段响应草稿 {checkinDate,cumulativeDays,streakCurrent}——三字段会在跨阈值打卡时
/// 造成里程碑徽标已 achieved 而 statusText 停留旧值的自相矛盾,前端被契约禁止自算业务文案,
/// 详见 fe-contract.md POST /checkin 节的审查裁决)。
class CheckinEndpointTest {

    private final CommandBus commandBus = mock(CommandBus.class);
    private final CheckinStatusQueryService statusQuery = mock(CheckinStatusQueryService.class);
    private final CheckinRewardQueryService rewardQuery = mock(CheckinRewardQueryService.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ActivityController controller = new ActivityController(
                commandBus, mock(DrawStatusQueryService.class), mock(LeaderboardQueryService.class),
                mock(RecentRechargesQueryService.class), mock(PoolQueryService.class),
                mock(RecentDrawsQueryService.class), mock(MyDrawsQueryService.class),
                mock(ReferralLeaderboardQueryService.class), mock(UsageLeaderboardQueryService.class),
                mock(RaffleQueryService.class), mock(RegistryStatusQueryService.class),
                statusQuery, rewardQuery, mock(AchievementWallQueryService.class));
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(42, "user", "active")));
    }

    /// 共用状态快照构造(仅 punchedToday 随场景变化):两个端点(POST 成功后 / GET status)
    /// 复用同一 CheckinStatusQueryService,故契约测试里状态快照的形状本就该相同。
    private static CheckinStatusView statusView(boolean punchedToday) {
        var supply = new CheckinSupplyView(new BigDecimal("36"), 43, "距 B 档还差 ¥14", List.of(
                new CheckinSupplyTierView("A", "GPT-Plus 补给 · 3 天", "…", new BigDecimal("30"),
                        "armed", "充值已达标 · 满勤在轨"),
                new CheckinSupplyTierView("B", "GPT-Pro 补给 · 3 天", "…", new BigDecimal("50"),
                        "progress", "差 ¥14"),
                new CheckinSupplyTierView("C", "GPT-Pro 补给 · 7 天", "…", new BigDecimal("500"),
                        "dim", "未在轨")));
        var milestones = List.of(
                new CheckinMilestoneView("days_5", "出勤 5 天", 5, true, "已打穿"),
                new CheckinMilestoneView("days_10", "出勤 10 天", 10, true, "已打穿"),
                new CheckinMilestoneView("days_20", "出勤 20 天", 20, false, "12 / 20"),
                new CheckinMilestoneView("full_month", "满勤", 31, false, "在轨 · 一格没漏"));
        return new CheckinStatusView(
                true, null, punchedToday, 13, "2026.07", 31, List.of(1, 2, 3), 12, 12, milestones, supply, 235);
    }

    @Test
    void checkInRequiresLoginAndReturnsFullStatusSnapshot() throws Exception {
        when(commandBus.handle(new CheckInCommand(42)))
                .thenReturn(new CheckInResult(LocalDate.of(2026, 7, 13), 13, 13));
        when(statusQuery.status(42)).thenReturn(statusView(true));

        mvc.perform(post("/activity/v1/checkin").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.punchedToday").value(true))
                .andExpect(jsonPath("$.cumulativeDays").value(12))
                .andExpect(jsonPath("$.streakCurrent").value(12))
                .andExpect(jsonPath("$.nbTotal").value(235))
                .andExpect(jsonPath("$.milestones[0].code").value("days_5"))
                .andExpect(jsonPath("$.milestones[0].statusText").value("已打穿"))
                .andExpect(jsonPath("$.supply.tiers[1].state").value("progress"))
                .andExpect(jsonPath("$.supply.gaugeNote").value("距 B 档还差 ¥14"));
        verify(commandBus).handle(new CheckInCommand(42));
    }

    @Test
    void statusRequiresLoginAndReturnsContractShape() throws Exception {
        when(statusQuery.status(42)).thenReturn(statusView(false));

        mvc.perform(get("/activity/v1/checkin/status").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.ineligibleReason").value(nullValue())) // Jackson 仍输出 null 字段
                .andExpect(jsonPath("$.punchedToday").value(false))
                .andExpect(jsonPath("$.todayDay").value(13))
                .andExpect(jsonPath("$.monthLabel").value("2026.07"))
                .andExpect(jsonPath("$.checkedDays[0]").value(1))
                .andExpect(jsonPath("$.milestones[0].code").value("days_5"))
                .andExpect(jsonPath("$.milestones[0].statusText").value("已打穿"))
                .andExpect(jsonPath("$.supply.tiers[1].state").value("progress"))
                .andExpect(jsonPath("$.supply.gaugeNote").value("距 B 档还差 ¥14"));
    }

    @Test
    void rewardsRequiresLoginAndReturnsGrantsWrapper() throws Exception {
        when(rewardQuery.myRewards(42)).thenReturn(List.of(
                new CheckinRewardSummary("2026-06", "B", "GPT-Pro 补给 · 3 天",
                        Instant.parse("2026-06-30T15:58:00Z"))));

        mvc.perform(get("/activity/v1/checkin/rewards").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grants[0].month").value("2026-06"))
                .andExpect(jsonPath("$.grants[0].tier").value("B"))
                .andExpect(jsonPath("$.grants[0].label").value("GPT-Pro 补给 · 3 天"))
                // 契约示例 "2026-06-30T23:58:00+08:00":Instant 转 Asia/Shanghai 偏移量而非默认 UTC "Z"
                .andExpect(jsonPath("$.grants[0].grantedAt").value("2026-06-30T23:58:00+08:00"));
    }
}
