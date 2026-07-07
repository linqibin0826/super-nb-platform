package me.supernb.boot;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import me.supernb.gallery.app.ImageStoragePort;
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

/// 灵感库全栈装配:真实上下文 + Testcontainers PG + Flyway 建 gallery schema。
/// R2 未配(mock ImageStoragePort);验证公开端点可达、未带 token 401、写请求真经 CommandBus 派发。
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class GalleryWiringTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("sub2api.read-datasource.url", PG::getJdbcUrl);
        r.add("sub2api.read-datasource.username", PG::getUsername);
        r.add("sub2api.read-datasource.password", PG::getPassword);
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ImageStoragePort imageStoragePort;

    // mock introspect:未 stub 时返回 Optional.empty → 401;stub 后放行真派发路径
    @MockitoBean
    Sub2apiIntrospectClient introspect;

    @Test
    void categoriesIsPublicAndReturnsThreeAxes() throws Exception {
        mvc.perform(get("/gallery/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scene").isArray())
                .andExpect(jsonPath("$.style").isArray())
                .andExpect(jsonPath("$.subject").isArray());
    }

    @Test
    void promptsPublicEmpty() throws Exception {
        mvc.perform(get("/gallery/v1/prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void myFavoritesWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(get("/gallery/v1/me/favorites"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGenerationWithoutTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/gallery/v1/me/generations")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"x\",\"prompt\":\"p\",\"size\":\"1024x1024\",\"n\":1,\"quality\":\"medium\",\"status\":\"done\",\"elapsedMs\":0}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void likeWithTokenDispatchesThroughCommandBus() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        // 空库无该 prompt:请求穿过 解析器→控制器→CommandBus→TogglePromptLikeHandler→真 PG,按契约 404
        mvc.perform(post("/gallery/v1/prompts/999/like").header("Authorization", "Bearer T"))
                .andExpect(status().isNotFound());
    }
}
