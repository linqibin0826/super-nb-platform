package me.supernb.boot;

import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import org.junit.jupiter.api.Test;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// 内容中心全栈装配：真实上下文 + Testcontainers PG + Flyway 建 content schema。
/// CommandBus 路由表是运行期按泛型动态构建的，「接对了」必须一次真实派发验证（CONTRIBUTING 新上下文四步之一）：
/// 本测试的发布往返用例即从 Controller 穿 CommandBus → Handler → 真实 PG 再读回。
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ContentWiringTest {

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
        r.add("content.admin-token", () -> "test-token");
    }

    @Autowired
    MockMvc mvc;

    // R2 未配（照 GalleryWiringTest：全栈上下文连 gallery 一起拉起，storage 端口用 mock 挡）
    @MockitoBean
    ImageStoragePort imageStoragePort;

    @MockitoBean
    Sub2apiIntrospectClient introspect;

    @Test
    void categoriesIsPublicAndEmpty() throws Exception {
        mvc.perform(get("/content/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void adminWithoutTokenIs401() throws Exception {
        mvc.perform(put("/content/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminWithWrongTokenIs401() throws Exception {
        mvc.perform(put("/content/v1/admin/categories").header("X-Admin-Token", "nope")
                        .contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownSlugIs404ProblemJson() throws Exception {
        mvc.perform(get("/content/v1/articles/no-such"))
                .andExpect(status().isNotFound());
    }

    @Test
    void fullPublishRoundTrip() throws Exception {
        mvc.perform(put("/content/v1/admin/categories").header("X-Admin-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"slug\":\"tutorials\",\"name\":\"教程\",\"sortOrder\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upserted").value(1));

        mvc.perform(post("/content/v1/admin/articles:upsert").header("X-Admin-Token", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"hello-codex","type":"article","title":"你好 Codex","summary":"s",
                                 "categorySlug":"tutorials","tags":["Codex"],"bodyHtml":"<p>hi</p>",
                                 "publishedAt":"2026-07-10T00:00:00Z","hidden":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(true));

        mvc.perform(get("/content/v1/articles/hello-codex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bodyHtml").value("<p>hi</p>"))
                .andExpect(jsonPath("$.categoryName").value("教程"));

        mvc.perform(get("/content/v1/articles").param("category", "tutorials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].slug").value("hello-codex"));
    }
}
