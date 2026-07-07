package me.supernb.gallery.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.gallery.app.usecase.generation.command.CreateGenerationCommand;
import me.supernb.gallery.app.usecase.generation.dto.Created;
import me.supernb.gallery.app.usecase.generation.query.GenerationQueries;
import me.supernb.gallery.app.usecase.interaction.command.TogglePromptLikeCommand;
import me.supernb.gallery.app.usecase.interaction.dto.LikeResult;
import me.supernb.gallery.app.usecase.interaction.query.InteractionQueries;
import me.supernb.gallery.app.usecase.prompt.query.PromptQueries;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 灵感库控制器映射 + JSON 契约(standalone MockMvc,happy path)。
/// 写端点 mock CommandBus——命令是 record,equals 精确匹配即断言了派发参数。
class GalleryControllerTest {

    private final CommandBus commandBus = mock(CommandBus.class);
    private final PromptQueries promptQueries = mock(PromptQueries.class);
    private final InteractionQueries interactionQueries = mock(InteractionQueries.class);
    private final GenerationQueries generationQueries = mock(GenerationQueries.class);
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new GalleryController(
                        commandBus, promptQueries, interactionQueries, generationQueries))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void listPromptsIsPublic() throws Exception {
        when(promptQueries.list("style", null, "featured", 1, 24)).thenReturn(
                Page.of(List.of(new PromptSummary(
                        1, "a cat", "http://img", 512, 512, "alice", 3, 1)), 1, 1, 24));
        mvc.perform(get("/gallery/v1/prompts").param("category", "style"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("a cat"))
                .andExpect(jsonPath("$.items[0].likeCount").value(3));
    }

    @Test
    void likeRequiresTokenAndDispatchesCommand() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(commandBus.handle(new TogglePromptLikeCommand(5L, 7L, true)))
                .thenReturn(new LikeResult(4, true));
        mvc.perform(post("/gallery/v1/prompts/5/like").header("Authorization", "Bearer T"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likeCount").value(4))
                .andExpect(jsonPath("$.liked").value(true));
    }

    @Test
    void createGenerationDecodesBase64AndDispatchesCommand() throws Exception {
        when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        when(commandBus.handle(any(CreateGenerationCommand.class)))
                .thenReturn(new Created("task-1", Instant.parse("2026-07-06T00:00:00Z")));
        String body = "{\"id\":\"task-1\",\"prompt\":\"p\",\"size\":\"1024x1024\",\"n\":1,"
                + "\"quality\":\"medium\",\"status\":\"done\",\"elapsedMs\":0,"
                + "\"outputImages\":[{\"b64\":\"AQID\"}]}";
        mvc.perform(post("/gallery/v1/me/generations")
                        .header("Authorization", "Bearer T")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-1"));

        ArgumentCaptor<CreateGenerationCommand> dispatched = ArgumentCaptor.forClass(CreateGenerationCommand.class);
        verify(commandBus).handle(dispatched.capture());
        assertThat(dispatched.getValue().userId()).isEqualTo(7L);
        assertThat(dispatched.getValue().outputImages()).hasSize(1);
        assertThat(dispatched.getValue().outputImages().get(0).data()).containsExactly(1, 2, 3);
    }
}
