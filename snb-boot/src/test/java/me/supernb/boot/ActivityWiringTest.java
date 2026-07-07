package me.supernb.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 活动上下文全栈装配:真实 Spring 上下文 + Testcontainers PG + Flyway 建 activity schema。
/// 验证公开端点可达、登录端点未带 token 走 commons 错误处理返回 401。
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
}
