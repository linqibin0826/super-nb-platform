package me.supernb.boot;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 活动上下文全栈装配:真实 Spring 上下文 + Testcontainers PG + Flyway 建 activity schema。
/// 验证公开端点可达、未带 token 401、带 token 的写请求真经 CommandBus 派发到 Handler。
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ActivityWiringTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        // sub2api 只读源在本测试指向同一容器(仅需能连上;登录端点未带 token 会先短路)
        r.add("sub2api.read-datasource.url", PG::getJdbcUrl);
        r.add("sub2api.read-datasource.username", PG::getUsername);
        r.add("sub2api.read-datasource.password", PG::getPassword);
    }

    @Autowired
    MockMvc mvc;

    // gallery 生成服务依赖 R2(测试不配),提供 mock 让全上下文可装配
    @MockitoBean
    ImageStoragePort imageStoragePort;

    // mock introspect:未 stub 时返回 Optional.empty → 401;stub 后放行真派发路径
    @MockitoBean
    Sub2apiIntrospectClient introspect;

    @Autowired
    JdbcTemplate jdbc;

    /// 最小桩表(签到状态快照需要):users 满足账龄门槛(RaffleGateReadModel.registeredAts),
    /// payment_orders/redeem_codes 满足补给资格充值口径(RaffleGateReadModel.gateValue——
    /// CheckinStatusQueryService 组装 supply 进度预览时调用,详见 checkInSucceedsOnce...测试
    /// 首次真实执行才暴露的依赖:standalone 单测从不触达这条只读链路)。真实 sub2api 库不归
    /// 本仓管,这里只补三张形状最小的桩表满足既有 SQL 假设,不插入行(COALESCE 对空表归零)。
    @BeforeEach
    void ensureUsersTableStub() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, created_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "order_type TEXT, status TEXT, amount NUMERIC(20,2), completed_at TIMESTAMPTZ, "
                + "recharge_code TEXT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS redeem_codes (id BIGSERIAL PRIMARY KEY, code TEXT UNIQUE, "
                + "type TEXT, value NUMERIC(20,8), status TEXT, used_by BIGINT, used_at TIMESTAMPTZ)");
    }

    @Test
    void poolIsPublicAndEmptyWhenNoActiveCampaign() throws Exception {
        mvc.perform(get("/activity/v1/pool"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void statusWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/activity/v1/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gateDrawWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/activity/v1/gate/draw")).andExpect(status().isUnauthorized());
    }

    @Test
    void registryStatusIsPublicAndAlwaysCarriesEvergreenEntries() throws Exception {
        // 空库无 lottery/raffle 期 → 两条目缺席不断言;qq 状态值随测试运行日期变化,只断言存在性
        mvc.perform(get("/activity/v1/registry-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaigns[*].id").value(hasItem("leaderboard")))
                .andExpect(jsonPath("$.campaigns[*].id").value(hasItem("qq-referral")))
                .andExpect(jsonPath("$.campaigns[?(@.kind=='evergreen')].status").value(hasItem("running")));
    }

    @Test
    void drawWithTokenDispatchesThroughCommandBus() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        // 空库无进行中活动:请求穿过 解析器→控制器→CommandBus→PerformDrawHandler→真 PG,按契约 404
        mvc.perform(post("/activity/v1/draw").header("Authorization", "Bearer T"))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkInWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/activity/v1/checkin")).andExpect(status().isUnauthorized());
    }

    @Test
    void checkInWithTooYoungAccountIsForbidden() throws Exception {
        jdbc.update("INSERT INTO users (id, created_at) VALUES (501, now() - interval '1 hour') "
                + "ON CONFLICT (id) DO UPDATE SET created_at = EXCLUDED.created_at");
        when(introspect.introspect("Bearer YOUNG")).thenReturn(Optional.of(new UserProfile(501, "user", "active")));
        mvc.perform(post("/activity/v1/checkin").header("Authorization", "Bearer YOUNG"))
                .andExpect(status().isForbidden());
    }

    @Test
    void checkInSucceedsOnceThenConflictsOnSecondAttemptSameDay() throws Exception {
        jdbc.update("INSERT INTO users (id, created_at) VALUES (502, now() - interval '48 hours') "
                + "ON CONFLICT (id) DO UPDATE SET created_at = EXCLUDED.created_at");
        when(introspect.introspect("Bearer OLD")).thenReturn(Optional.of(new UserProfile(502, "user", "active")));
        mvc.perform(post("/activity/v1/checkin").header("Authorization", "Bearer OLD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cumulativeDays").value(1))
                .andExpect(jsonPath("$.streakCurrent").value(1));
        mvc.perform(post("/activity/v1/checkin").header("Authorization", "Bearer OLD"))
                .andExpect(status().isConflict());
    }
}
