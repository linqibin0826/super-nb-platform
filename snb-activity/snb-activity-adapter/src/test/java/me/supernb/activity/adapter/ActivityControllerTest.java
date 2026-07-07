package me.supernb.activity.adapter;

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
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.GetDrawStatusUseCase;
import me.supernb.activity.app.GetLeaderboardUseCase;
import me.supernb.activity.app.GetMyDrawsUseCase;
import me.supernb.activity.app.GetPoolUseCase;
import me.supernb.activity.app.GetRecentDrawsUseCase;
import me.supernb.activity.app.GetRecentRechargesUseCase;
import me.supernb.activity.app.PerformDrawCommand;
import me.supernb.activity.domain.DrawResult;
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
    private final GetDrawStatusUseCase getDrawStatus = mock(GetDrawStatusUseCase.class);
    private final GetLeaderboardUseCase getLeaderboard = mock(GetLeaderboardUseCase.class);
    private final GetRecentRechargesUseCase getRecentRecharges = mock(GetRecentRechargesUseCase.class);
    private final GetPoolUseCase getPool = mock(GetPoolUseCase.class);
    private final GetRecentDrawsUseCase getRecentDraws = mock(GetRecentDrawsUseCase.class);
    private final GetMyDrawsUseCase getMyDraws = mock(GetMyDrawsUseCase.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ActivityController controller = new ActivityController(
                commandBus, getDrawStatus, getLeaderboard, getRecentRecharges,
                getPool, getRecentDraws, getMyDraws);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void poolIsPublicAndReturnsTiers() throws Exception {
        when(getPool.pool()).thenReturn(List.of(new ActivityDto.PoolTier(new BigDecimal("10"), 5, 3)));
        mvc.perform(get("/activity/v1/pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(10))
                .andExpect(jsonPath("$[0].total").value(5))
                .andExpect(jsonPath("$[0].available").value(3));
    }

    @Test
    void statusWithValidTokenReturnsEligibility() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(getDrawStatus.status(7)).thenReturn(new ActivityDto.DrawStatus(true, 2));
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
}
