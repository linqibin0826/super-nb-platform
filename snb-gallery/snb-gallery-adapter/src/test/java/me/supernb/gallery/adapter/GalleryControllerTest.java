package me.supernb.gallery.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.gallery.app.GalleryDto;
import me.supernb.gallery.app.Generations;
import me.supernb.gallery.app.Interactions;
import me.supernb.gallery.app.PromptQueries;
import me.supernb.sub2api.Sub2apiIntrospectClient;
import me.supernb.sub2api.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 灵感库控制器映射 + JSON 契约(standalone MockMvc,happy path)。
class GalleryControllerTest {

    private final PromptQueries promptQueries = mock(PromptQueries.class);
    private final Interactions interactions = mock(Interactions.class);
    private final Generations generations = mock(Generations.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(
                new GalleryController(promptQueries, interactions, generations, introspect)).build();
    }

    @Test
    void listPromptsIsPublic() throws Exception {
        when(promptQueries.list("style", null, "featured", 1, 24)).thenReturn(
                GalleryDto.Page.of(List.of(new GalleryDto.PromptSummary(
                        1, "a cat", "http://img", 512, 512, "alice", 3, 1)), 1, 1, 24));
        mvc.perform(get("/gallery/v1/prompts").param("category", "style"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("a cat"))
                .andExpect(jsonPath("$.items[0].likeCount").value(3));
    }

    @Test
    void likeRequiresTokenAndReturnsCount() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(interactions.like(5L, 7L, true)).thenReturn(new Interactions.LikeResult(4, true));
        mvc.perform(post("/gallery/v1/prompts/5/like").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(4))
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    void createGenerationDecodesBase64AndReturnsId() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(generations.create(any())).thenReturn(new Generations.Created("task-1", Instant.parse("2026-07-06T00:00:00Z")));
        String body = "{\"id\":\"task-1\",\"prompt\":\"p\",\"size\":\"1024x1024\",\"n\":1,"
                + "\"quality\":\"medium\",\"status\":\"done\",\"elapsedMs\":0,"
                + "\"outputImages\":[{\"b64\":\"AQID\"}]}";
        mvc.perform(post("/gallery/v1/me/generations")
                        .header("Authorization", "Bearer T")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-1"));
    }
}
