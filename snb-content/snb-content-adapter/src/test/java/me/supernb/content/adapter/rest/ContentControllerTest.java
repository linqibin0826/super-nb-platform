package me.supernb.content.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import me.supernb.content.app.usecase.article.ArticleQueries;
import me.supernb.content.app.usecase.article.command.UpsertArticleCommand;
import me.supernb.content.app.usecase.article.dto.UpsertResult;
import me.supernb.content.app.usecase.category.command.SyncCategoriesCommand;
import me.supernb.content.app.usecase.category.dto.SyncResult;
import me.supernb.content.domain.model.read.ArticleSummary;
import me.supernb.content.domain.model.read.CategoryView;
import me.supernb.content.domain.model.read.Page;
import me.supernb.content.domain.port.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// 内容控制器映射 + JSON 契约（standalone MockMvc）。写端点 mock CommandBus——命令是 record，
/// equals 精确匹配即断言了派发参数。
class ContentControllerTest {

    private final CommandBus commandBus = mock(CommandBus.class);
    private final ArticleQueries queries = mock(ArticleQueries.class);

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new ContentController(queries, commandBus)).build();
    }

    ArticleSummary summary(String slug) {
        return new ArticleSummary("42", slug, "article", "你好 Codex", "s", null,
                "tutorials", "教程", List.of("Codex"), null, Instant.parse("2026-07-10T00:00:00Z"));
    }

    @Test
    void listClampsPageAndPageSize() throws Exception {
        when(queries.list("tutorials", "MCP", 1, 48)).thenReturn(Page.of(List.of(), 0, 1, 48));

        mvc.perform(get("/content/v1/articles")
                        .param("category", "tutorials").param("tag", "MCP")
                        .param("page", "0").param("pageSize", "999"))
                .andExpect(status().isOk());

        verify(queries).list("tutorials", "MCP", 1, 48); // page 钳到 1、pageSize 钳到 48
    }

    @Test
    void listSerializesCamelCaseEnvelope() throws Exception {
        when(queries.list(null, null, 1, 12)).thenReturn(Page.of(List.of(summary("hello-codex")), 1, 1, 12));

        mvc.perform(get("/content/v1/articles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].categoryName").value("教程"))
                .andExpect(jsonPath("$.items[0].publishedAt").exists())
                .andExpect(jsonPath("$.items[0].id").value("42"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pages").value(1));
    }

    @Test
    void categoriesIsPublic() throws Exception {
        when(queries.categories()).thenReturn(List.of(new CategoryView("tutorials", "教程", 1, 2L)));

        mvc.perform(get("/content/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("tutorials"))
                .andExpect(jsonPath("$[0].count").value(2));
    }

    @Test
    void upsertDispatchesCommandWithExactFields() throws Exception {
        when(commandBus.handle(new UpsertArticleCommand("hello-codex", "article", "你好 Codex", "s", null,
                "tutorials", List.of("Codex"), "<p>hi</p>", null, null, null,
                Instant.parse("2026-07-10T00:00:00Z"), false)))
                .thenReturn(new UpsertResult("42", true));

        mvc.perform(post("/content/v1/admin/articles:upsert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"hello-codex","type":"article","title":"你好 Codex","summary":"s",
                                 "categorySlug":"tutorials","tags":["Codex"],"bodyHtml":"<p>hi</p>",
                                 "publishedAt":"2026-07-10T00:00:00Z","hidden":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("42"))
                .andExpect(jsonPath("$.created").value(true));
    }

    @Test
    void syncCategoriesDispatchesCommand() throws Exception {
        when(commandBus.handle(new SyncCategoriesCommand(List.of(
                new CategoryRepository.CategoryData("tutorials", "教程", 1)))))
                .thenReturn(new SyncResult(1, 0));

        mvc.perform(put("/content/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"slug\":\"tutorials\",\"name\":\"教程\",\"sortOrder\":1}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upserted").value(1))
                .andExpect(jsonPath("$.deleted").value(0));
    }
}
