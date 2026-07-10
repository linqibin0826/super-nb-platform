package me.supernb.activity.adapter.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.campaign.query.LeaderboardQueryService;
import me.supernb.activity.app.usecase.campaign.query.PoolQueryService;
import me.supernb.activity.app.usecase.campaign.query.RecentRechargesQueryService;
import me.supernb.activity.app.usecase.draw.query.DrawStatusQueryService;
import me.supernb.activity.app.usecase.draw.query.MyDrawsQueryService;
import me.supernb.activity.app.usecase.draw.query.RecentDrawsQueryService;
import me.supernb.activity.app.usecase.referral.query.ReferralLeaderboardQueryService;
import me.supernb.activity.app.usecase.usageboard.BoardAssembler;
import me.supernb.activity.app.usecase.usageboard.BoardDataset;
import me.supernb.activity.app.usecase.usageboard.UsageBoardCache;
import me.supernb.activity.app.usecase.usageboard.UsageLeaderboardQueryService;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// usage-leaderboard 端点契约(standalone MockMvc):camelCase 字段、他人行无精确金额、
/// me 含本人精确值、非法参数 400、冷缓存 503。数据集用真组装器造,只 mock 缓存与读端口。
class UsageLeaderboardEndpointTest {

    private final UsageBoardCache cache = mock(UsageBoardCache.class);
    private final UsageBoardReadPort readPort = mock(UsageBoardReadPort.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        UsageLeaderboardQueryService svc = new UsageLeaderboardQueryService(cache, readPort);
        ActivityController controller = new ActivityController(
                mock(CommandBus.class), mock(DrawStatusQueryService.class), mock(LeaderboardQueryService.class),
                mock(RecentRechargesQueryService.class), mock(PoolQueryService.class),
                mock(RecentDrawsQueryService.class), mock(MyDrawsQueryService.class),
                mock(ReferralLeaderboardQueryService.class), svc);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(42, "user", "active")));
    }

    /// 两行真实数据:榜一 12000 刀(T_10K),我(uid42)第 2、精确 58.2。
    private static BoardDataset dataset() {
        return BoardAssembler.assemble(
                List.of(new UsageBoardRow(1, "老王", null, 1000, 10, 12000.0),
                        new UsageBoardRow(42, "12***67@qq.com", null, 500, 5, 58.2)),
                Map.of(), Map.of(),
                Instant.parse("2026-07-10T10:00:00Z"), Instant.parse("2026-07-12T16:00:00Z"));
    }

    @Test
    void returnsCamelCaseBoardWithMeAndWithoutOthersCost() throws Exception {
        when(cache.dataset(BoardPeriod.WEEK)).thenReturn(dataset());
        mvc.perform(get("/activity/v1/usage-leaderboard")
                        .param("period", "week").param("metric", "tokens")
                        .header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("week"))
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.participants").value(2))
                .andExpect(jsonPath("$.top[0].displayName").value("老王"))
                .andExpect(jsonPath("$.top[0].costTier").value("T_10K"))
                .andExpect(jsonPath("$.top[0].cost").doesNotExist())      // ?? 他人精确金额不得出现
                .andExpect(jsonPath("$.me.rank").value(2))
                .andExpect(jsonPath("$.me.cost").value(58.2))             // 本人精确值可见
                .andExpect(jsonPath("$.me.gapToNext.tokens").value(500))
                .andExpect(jsonPath("$.meStatus").value(Matchers.nullValue())); // me 在榜时为 null(Jackson 仍输出 null 字段)
    }

    @Test
    void invalidPeriodRejectedWith400() throws Exception {
        mvc.perform(get("/activity/v1/usage-leaderboard")
                        .param("period", "year").param("metric", "tokens")
                        .header("Authorization", "Bearer T"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void coldCacheYields503() throws Exception {
        when(cache.dataset(BoardPeriod.DAY)).thenReturn(null);
        mvc.perform(get("/activity/v1/usage-leaderboard")
                        .param("period", "day").param("metric", "amount")
                        .header("Authorization", "Bearer T"))
                .andExpect(status().isServiceUnavailable());
    }
}
