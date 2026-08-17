package me.supernb.boot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.JsonPath;
import java.util.Base64;
import java.util.Optional;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// ops 全栈装配:真实上下文 + Testcontainers PG(主库跑 Flyway 全部 schema),测试假钥走
/// DynamicPropertySource。走完整闭环:建号(带密码)→列表无明文→解密→建订阅→看板命中→
/// 带订阅删号被拒(409)→删订阅→删号;外加 401/403。
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpsWiringTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("ops.secret-key", () -> Base64.getEncoder().encodeToString(new byte[32])); // 测试假钥
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ImageStoragePort imageStoragePort;   // R2 未配,照 ContentWiringTest 挡掉
    @MockitoBean
    Sub2apiIntrospectClient introspect;

    static String accountId;
    static String subscriptionId;

    void stubAuth() {
        when(introspect.introspect("Bearer admin-token"))
                .thenReturn(Optional.of(new UserProfile(1, "admin", "active")));
        when(introspect.introspect("Bearer user-token"))
                .thenReturn(Optional.of(new UserProfile(7, "user", "active")));
    }

    @Test
    @Order(1)
    void anonymousIsRejected() throws Exception {
        stubAuth();
        mvc.perform(get("/ops/v1/admin/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void nonAdminIsForbidden() throws Exception {
        stubAuth();
        mvc.perform(get("/ops/v1/admin/accounts").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void adminCreatesAccountAndListCarriesNoPlaintext() throws Exception {
        stubAuth();
        var res = mvc.perform(post("/ops/v1/admin/accounts").header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wiring@gmail.com\",\"provider\":\"gmail\","
                                + "\"password\":\"top-secret-pw\",\"owner\":\"林琪斌\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andReturn();
        accountId = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/ops/v1/admin/accounts").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("wiring@gmail.com"))
                .andExpect(jsonPath("$[0].hasPassword").value(true))
                .andExpect(content().string(Matchers.not(Matchers.containsString("top-secret-pw"))));
    }

    @Test
    @Order(4)
    void secretEndpointDecryptsPassword() throws Exception {
        stubAuth();
        mvc.perform(get("/ops/v1/admin/accounts/" + accountId + "/secret")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("top-secret-pw"));
    }

    @Test
    @Order(5)
    void subscriptionShowsUpOnDashboard() throws Exception {
        stubAuth();
        String nextBilling = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(5).toString();
        var res = mvc.perform(post("/ops/v1/admin/subscriptions").header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"" + accountId + "\",\"service\":\"CHATGPT\","
                                + "\"tier\":\"PRO\",\"region\":\"美区\",\"cardPlatform\":\"mekelove\","
                                + "\"cardLast4\":\"4732\",\"status\":\"ACTIVE\",\"nextBillingAt\":\""
                                + nextBilling + "\",\"priceUsd\":\"200.00\"}"))
                .andExpect(status().isOk())
                .andReturn();
        subscriptionId = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        mvc.perform(get("/ops/v1/admin/dashboard").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingBilling[0].email").value("wiring@gmail.com"))
                .andExpect(jsonPath("$.upcomingBilling[0].nextBillingAt").value(nextBilling))
                .andExpect(jsonPath("$.bannedOpenCount").value(0));
    }

    @Test
    @Order(6)
    void deletingAccountWithSubscriptionsIsRejected() throws Exception {
        stubAuth();
        mvc.perform(delete("/ops/v1/admin/accounts/" + accountId).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(7)
    void deleteSubscriptionThenAccountSucceeds() throws Exception {
        stubAuth();
        mvc.perform(delete("/ops/v1/admin/subscriptions/" + subscriptionId)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        mvc.perform(delete("/ops/v1/admin/accounts/" + accountId).header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        mvc.perform(get("/ops/v1/admin/accounts").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
