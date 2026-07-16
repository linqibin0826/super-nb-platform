package me.supernb.boot;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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

/// 通用引导已读全栈装配:401 把门 / 非法 key 422 / ack→回读 / 重复 ack 幂等。
/// mock 阵容照 InvoiceWiringTest(introspect/余额/R2 全挡,不碰任何真实外设)。
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GuideWiringTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("sub2api.read-datasource.url", PG::getJdbcUrl);
        r.add("sub2api.read-datasource.username", PG::getUsername);
        r.add("sub2api.read-datasource.password", PG::getPassword);
        r.add("sub2api.admin-key", () -> "test-admin-key");
        r.add("content.admin-token", () -> "test-token");
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ImageStoragePort imageStoragePort;
    @MockitoBean
    Sub2apiIntrospectClient introspect;
    @MockitoBean
    Sub2apiAdminBalanceClient balanceClient;

    void stubAuth() {
        when(introspect.introspect("Bearer user-token"))
                .thenReturn(Optional.of(new UserProfile(101, "user", "active")));
    }

    @Test
    @Order(1)
    void anonymousIs401() throws Exception {
        mvc.perform(get("/guide/v1/acks")).andExpect(status().isUnauthorized());
        mvc.perform(post("/guide/v1/acks/invoice.intro.v1")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void invalidKeyIs422() throws Exception {
        stubAuth();
        mvc.perform(post("/guide/v1/acks/BAD__KEY!").header("Authorization", "Bearer user-token"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @Order(3)
    void ackThenReadBackAndIdempotent() throws Exception {
        stubAuth();
        mvc.perform(get("/guide/v1/acks").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys.length()").value(0));
        mvc.perform(post("/guide/v1/acks/invoice.intro.v1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/guide/v1/acks/invoice.intro.v1").header("Authorization", "Bearer user-token"))
                .andExpect(status().isNoContent()); // 幂等
        mvc.perform(get("/guide/v1/acks").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys.length()").value(1))
                .andExpect(jsonPath("$.keys[0]").value("invoice.intro.v1"));
    }
}
