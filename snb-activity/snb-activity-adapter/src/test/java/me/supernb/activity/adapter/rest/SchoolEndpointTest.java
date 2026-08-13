package me.supernb.activity.adapter.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.achievement.config.AchievementProperties;
import me.supernb.activity.app.usecase.achievement.query.AchievementWallQueryService;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinRewardQueryService;
import me.supernb.activity.app.usecase.checkin.query.CheckinStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.raffle.RaffleQueryService;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.registry.query.RegistryStatusQueryService;
import me.supernb.activity.app.usecase.school.SchoolStatusView;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolCardCommand;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolFirstChargeCommand;
import me.supernb.activity.app.usecase.school.command.ResetSchoolCardCommand;
import me.supernb.activity.app.usecase.school.query.SchoolLeaderboardQueryService;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.app.usecase.thursday.query.ThursdayBucketQueryService;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 包机五端点契约(standalone MockMvc,happy path)。401/409 状态码映射不在此断言
/// (standalone 装配无 commons 全局错误处理器,家族惯例),留给 boot 层 wiring 测试。
class SchoolEndpointTest {

    private final CommandBus commandBus = mock(CommandBus.class);
    private final SchoolStatusQueryService statusQuery = mock(SchoolStatusQueryService.class);
    private final SchoolLeaderboardQueryService boardQuery = mock(SchoolLeaderboardQueryService.class);
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
                mock(CheckinStatusQueryService.class), mock(CheckinRewardQueryService.class),
                mock(AchievementWallQueryService.class), new AchievementProperties(false, true),
                mock(ThursdayBucketQueryService.class), statusQuery, boardQuery);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(42, "user", "active")));
    }

    private static SchoolStatusView sampleView() {
        return new SchoolStatusView(true, "9月1日 00:00",
                new SchoolStatusView.FirstChargeBlock(true, true, 100, "50.00", "claimable"),
                new SchoolStatusView.InviteBlock(7,
                        new SchoolStatusView.CardBlock(2, "Plus", 100, 2, "Plus", 100, 3, 2, 777L),
                        false));
    }

    @Test
    void statusReturnsWhitelistedShape() throws Exception {
        when(statusQuery.view(42)).thenReturn(sampleView());

        mvc.perform(get("/activity/v1/school/status").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.endsAtLabel").value("9月1日 00:00"))
                .andExpect(jsonPath("$.firstCharge.tierCard").value(100))
                .andExpect(jsonPath("$.firstCharge.status").value("claimable"))
                .andExpect(jsonPath("$.invite.count").value(7))
                .andExpect(jsonPath("$.invite.card.tier").value(2))
                .andExpect(jsonPath("$.invite.card.tierName").value("Plus"))
                .andExpect(jsonPath("$.invite.card.cardAmount").value(100))
                .andExpect(jsonPath("$.invite.card.resetsAvailable").value(3))
                .andExpect(jsonPath("$.invite.kfcUnlocked").value(false));
    }

    @Test
    void firstChargeClaimDelegatesToCommandBus() throws Exception {
        when(commandBus.handle(new ClaimSchoolFirstChargeCommand(42))).thenReturn(sampleView());

        mvc.perform(post("/activity/v1/school/first-charge/claim").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstCharge.tierCard").value(100));
        verify(commandBus).handle(new ClaimSchoolFirstChargeCommand(42));
    }

    @Test
    void cardClaimDelegatesToCommandBus() throws Exception {
        when(commandBus.handle(new ClaimSchoolCardCommand(42))).thenReturn(sampleView());

        mvc.perform(post("/activity/v1/school/card/claim").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invite.card.tierName").value("Plus"));
        verify(commandBus).handle(new ClaimSchoolCardCommand(42));
    }

    @Test
    void resetDelegatesToCommandBus() throws Exception {
        when(commandBus.handle(new ResetSchoolCardCommand(42))).thenReturn(sampleView());

        mvc.perform(post("/activity/v1/school/reset").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invite.card.resetsAvailable").value(3));
        verify(commandBus).handle(new ResetSchoolCardCommand(42));
    }

    @Test
    void leaderboardIsPublicAndRanked() throws Exception {
        when(boardQuery.top(ArgumentMatchers.any())).thenReturn(List.of(
                new SchoolLeaderboardQueryService.Entry("zh***an@qq.com", 12, 20),
                new SchoolLeaderboardQueryService.Entry("***@gmail.com", 3, 3)));

        mvc.perform(get("/activity/v1/school/leaderboard"))   // 不带 Authorization:公开端点
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].rank").value(1))
                .andExpect(jsonPath("$.entries[0].name").value("zh***an@qq.com"))
                .andExpect(jsonPath("$.entries[0].count").value(12))
                .andExpect(jsonPath("$.entries[0].invited").value(20))
                .andExpect(jsonPath("$.entries[1].rank").value(2));
    }
}
