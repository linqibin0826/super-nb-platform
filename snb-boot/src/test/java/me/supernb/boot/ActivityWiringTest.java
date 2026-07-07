package me.supernb.boot;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import me.supernb.gallery.domain.port.ImageStoragePort;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

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
    void drawWithTokenDispatchesThroughCommandBus() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        // 空库无进行中活动:请求穿过 解析器→控制器→CommandBus→PerformDrawHandler→真 PG,按契约 404
        mvc.perform(post("/activity/v1/draw").header("Authorization", "Bearer T"))
                .andExpect(status().isNotFound());
    }
}
