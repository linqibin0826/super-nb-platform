package me.supernb.boot;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.util.Optional;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 开票全栈装配:真实上下文 + Testcontainers PG(主库跑 Flyway 全部 schema;同容器手建
/// sub2api fixture 表当 RO 源)。余额客户端 @MockitoBean 挡真扣费——**任何测试不打真生产 admin API**。
/// 走完整闭环:建抬头→总览→提交→admin 扣费(验 notes 带单号)→传 PDF→用户下载;外加 401/403。
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InvoiceWiringTest {

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
        r.add("sub2api.admin-key", () -> "test-admin-key"); // 让 admin 家族 Bean 装配,再被 MockitoBean 顶掉
        r.add("content.admin-token", () -> "test-token");
    }

    @BeforeAll
    static void seedUpstreamFixture() {
        var ds = new org.springframework.jdbc.datasource.DriverManagerDataSource(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, email TEXT, role TEXT, "
                + "balance DOUBLE PRECISION)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS payment_orders (id BIGINT PRIMARY KEY, user_id BIGINT, "
                + "amount NUMERIC(20,2), out_trade_no TEXT, order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");
        jdbc.update("INSERT INTO users VALUES (101,'alice@qq.com','user',100.0)");
        jdbc.update("INSERT INTO payment_orders VALUES (11,101,600,'T600','balance','COMPLETED',now())");
        jdbc.update("INSERT INTO payment_orders VALUES (12,101,500,'T500','balance','COMPLETED',now())");
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ImageStoragePort imageStoragePort;      // R2 未配,照 ContentWiringTest 挡掉
    @MockitoBean
    Sub2apiIntrospectClient introspect;
    @MockitoBean
    Sub2apiAdminBalanceClient balanceClient; // 绝不真扣费

    static String requestId;

    /// 只显式打两个有效 token;其余(含 null 头)靠 Mockito 对 Optional 返回类型的默认答案
    /// (ReturnsEmptyValues 特判)天然得到 Optional.empty(),不必费力构造 argThat(null 分支)。
    void stubAuth() {
        when(introspect.introspect("Bearer user-token"))
                .thenReturn(Optional.of(new UserProfile(101, "user", "active")));
        when(introspect.introspect("Bearer admin-token"))
                .thenReturn(Optional.of(new UserProfile(1, "admin", "active")));
    }

    @Test
    @Order(1)
    void anonymousIs401AndUserOnAdminIs403() throws Exception {
        stubAuth();
        mvc.perform(get("/invoice/v1/profiles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/invoice/v1/admin/requests").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(2)
    void fullInvoiceRoundTrip() throws Exception {
        stubAuth();
        // 建抬头
        String profileBody = mvc.perform(post("/invoice/v1/profiles")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"COMPANY\",\"title\":\"某某科技\",\"taxNo\":\"91330100X\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String profileId = JsonPath.read(profileBody, "$.id");

        // 总览:两单可选,合计 1100
        mvc.perform(get("/invoice/v1/orders").header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(2))
                .andExpect(jsonPath("$.billableTotal").value(1100.00));

        // 提交:fee=55
        String created = mvc.perform(post("/invoice/v1/requests")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[\"11\",\"12\"],\"profileId\":\"" + profileId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fee").value(55.00))
                .andReturn().getResponse().getContentAsString();
        requestId = JsonPath.read(created, "$.id");
        String requestNo = JsonPath.read(created, "$.requestNo");

        // 占用后总览清空 + 重复申请 409
        mvc.perform(get("/invoice/v1/orders").header("Authorization", "Bearer user-token"))
                .andExpect(jsonPath("$.orders.length()").value(0));
        mvc.perform(post("/invoice/v1/requests").header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[\"11\"],\"profileId\":\"" + profileId + "\"}"))
                .andExpect(status().isConflict());

        // admin 扣费受理:mock 客户端被以正确参数调用,状态转 INVOICING
        mvc.perform(post("/invoice/v1/admin/requests/" + requestId + "/charge")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVOICING"));
        verify(balanceClient).subtract(org.mockito.ArgumentMatchers.eq(101L),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("55.00")),
                org.mockito.ArgumentMatchers.contains(requestNo));

        // admin 传 PDF → ISSUED
        mvc.perform(multipart("/invoice/v1/admin/requests/" + requestId + "/pdf")
                        .file(new MockMultipartFile("file", "发票.pdf", "application/pdf",
                                "%PDF-1.4 real".getBytes()))
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());

        // 用户下载
        mvc.perform(get("/invoice/v1/requests/" + requestId + "/pdf")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 列表可见 ISSUED
        mvc.perform(get("/invoice/v1/requests").header("Authorization", "Bearer user-token"))
                .andExpect(jsonPath("$[0].status").value("ISSUED"));
    }
}
