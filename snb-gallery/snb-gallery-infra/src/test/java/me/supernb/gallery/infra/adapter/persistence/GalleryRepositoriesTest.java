package me.supernb.gallery.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.gallery.domain.model.enums.SortMode;
import me.supernb.gallery.domain.model.read.CategoryTree;
import me.supernb.gallery.domain.model.read.MyInteractions;
import me.supernb.gallery.domain.model.read.Page;
import me.supernb.gallery.domain.model.read.PromptDetail;
import me.supernb.gallery.domain.model.read.PromptSummary;
import me.supernb.gallery.domain.port.repository.GenerationRepository;
import me.supernb.gallery.infra.adapter.read.PromptReadAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// gallery 三适配器(JPA)对真实 Flyway schema(Testcontainers PG)的集成测试。
@SpringBootTest(classes = GalleryInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class GalleryRepositoriesTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/gallery");
        r.add("spring.flyway.schemas", () -> "gallery");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PromptReadAdapter prompts;

    @Autowired
    InteractionRepositoryAdapter interactions;

    @Autowired
    GenerationRepositoryAdapter generations;

    long p1;
    long p2;
    long p3;

    long nextPromptId;

    /// 造数:id 显式给值(雪花基座后无数据库自增,纯 SQL 写入必须带 id)。
    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE gallery.category, gallery.prompt, gallery.prompt_like, gallery.prompt_favorite, "
                + "gallery.generation, gallery.generation_image, gallery.generation_ref, gallery.ref_image");
        jdbc.update("INSERT INTO gallery.category (id, slug, axis, name_en, name_zh) VALUES "
                + "(1,'portrait','scene','Portrait','人像'),(2,'anime','style','Anime','动漫'),(3,'animal','subject','Animal','动物')");
        nextPromptId = 1;
        p1 = insertPrompt("s1", "a cat", "portrait");
        p2 = insertPrompt("s2", "a dog", "anime");
        p3 = insertPrompt("s3", "sunset", null);
    }

    long insertPrompt(String sourceId, String title, String categorySlug) {
        Long catId = categorySlug == null ? null
                : jdbc.queryForObject("SELECT id FROM gallery.category WHERE slug = ?", Long.class, categorySlug);
        return jdbc.queryForObject(
                "INSERT INTO gallery.prompt (id, source, source_id, title, prompt_text, author_name, category_id, image_w, image_h) "
                        + "VALUES (?, 'own', ?, ?, 'PROMPT', 'alice', ?, 512, 512) RETURNING id",
                Long.class, nextPromptId++, sourceId, title, catId);
    }

    @Test
    void listReturnsPublishedPaged() {
        Page<PromptSummary> page = prompts.list(null, null, SortMode.FEATURED, 1, 24);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.items()).hasSize(3);
        assertThat(page.items().get(0).id()).isEqualTo(p3); // featured = id DESC
    }

    @Test
    void listSupportsAllSortModes() {
        // 排序 HQL 为运行时动态拼接(启动期不校验),四种模式逐一走一遍确保可解析(含 NULLS LAST)
        for (SortMode mode : SortMode.values()) {
            assertThat(prompts.list(null, null, mode, 1, 24).total()).isEqualTo(3);
        }
    }

    @Test
    void listFiltersByCategoryAndSearch() {
        assertThat(prompts.list("portrait", null, SortMode.FEATURED, 1, 24).total()).isEqualTo(1);
        assertThat(prompts.list(null, "cat", SortMode.FEATURED, 1, 24).items().get(0).id()).isEqualTo(p1);
    }

    @Test
    void detailReturnsFullWithCategoryOrEmpty() {
        PromptDetail d = prompts.detail(p1).orElseThrow();
        assertThat(d.title()).isEqualTo("a cat");
        assertThat(d.category().slug()).isEqualTo("portrait");
        assertThat(prompts.detail(9999L)).isEmpty();
    }

    @Test
    void categoriesTreeHasCountsPerAxis() {
        CategoryTree tree = prompts.categories();
        assertThat(tree.scene()).anySatisfy(n -> {
            assertThat(n.slug()).isEqualTo("portrait");
            assertThat(n.count()).isEqualTo(1);
        });
        assertThat(tree.subject()).anySatisfy(n -> assertThat(n.count()).isZero());
    }

    @Test
    void likeToggleIsIdempotentAndCountsCorrectly() {
        assertThat(interactions.toggleLike(p1, 7L, true)).hasValue(1);
        assertThat(interactions.toggleLike(p1, 7L, true)).hasValue(1); // 幂等
        assertThat(interactions.toggleLike(p1, 8L, true)).hasValue(2);
        assertThat(interactions.toggleLike(p1, 7L, false)).hasValue(1);
        assertThat(interactions.toggleLike(9999L, 7L, true)).isEmpty(); // 404
    }

    @Test
    void favoritesAndMyInteractions() {
        interactions.toggleFavorite(p1, 7L, true);
        interactions.toggleLike(p2, 7L, true);

        Page<PromptSummary> favs = interactions.myFavorites(7L, 1, 24);
        assertThat(favs.total()).isEqualTo(1);
        assertThat(favs.items().get(0).id()).isEqualTo(p1);

        MyInteractions mine = interactions.myInteractions(List.of(p1, p2, p3), 7L);
        assertThat(mine.favorited()).containsExactly(p1);
        assertThat(mine.liked()).containsExactly(p2);
    }

    @Test
    void generationSaveListDetailDelete() {
        GenerationRepository.SaveGeneration cmd = new GenerationRepository.SaveGeneration(
                "g1", 7L, "a cat", "1024x1024", 1, "medium", "done", 0.04, 1200, "grp", 9L, null,
                "gen/7/g1/thumb.png",
                List.of(new GenerationRepository.OutputImage(0, "gen/7/g1/0.png", 100)),
                List.of(new GenerationRepository.RefImage(0, "shaA", "ref/7/shaA.png", 50)));
        generations.save(cmd);

        assertThat(generations.findCreatedAt("g1", 7L)).isPresent();
        assertThat(generations.refExists(7L, "shaA")).isTrue();

        GenerationRepository.PageRows list = generations.list(7L, 1, 24);
        assertThat(list.total()).isEqualTo(1);
        assertThat(list.rows().get(0).thumbKey()).isEqualTo("gen/7/g1/thumb.png");

        GenerationRepository.DetailRow detail = generations.detail("g1", 7L).orElseThrow();
        assertThat(detail.outputKeys()).containsExactly("gen/7/g1/0.png");
        assertThat(detail.refKeys()).containsExactly("ref/7/shaA.png");
        assertThat(generations.detail("g1", 999L)).isEmpty(); // 非本人

        Optional<List<String>> deletedKeys = generations.deleteReturningObjectKeys("g1", 7L);
        assertThat(deletedKeys).isPresent();
        assertThat(deletedKeys.get()).containsExactlyInAnyOrder("gen/7/g1/0.png", "gen/7/g1/thumb.png");
        assertThat(generations.findCreatedAt("g1", 7L)).isEmpty();
    }

    @Test
    void membershipRowsCarrySnowflakeIdAndAuditColumns() {
        interactions.toggleLike(p1, 7L, true);

        var row = jdbc.queryForMap(
                "SELECT id, created_at, updated_at, version FROM gallery.prompt_like WHERE prompt_id = ? AND user_id = 7", p1);
        assertThat(((Number) row.get("id")).longValue()).isGreaterThan(1_000_000_000L); // 雪花量级
        assertThat(row.get("created_at")).isNotNull(); // 点赞时刻,由审计填充
        assertThat(row.get("updated_at")).isNotNull();
        assertThat(((Number) row.get("version")).longValue()).isZero();
    }

    @Test
    void generationKeepsClientTaskIdAsExternalIdentity() {
        generations.save(new GenerationRepository.SaveGeneration(
                "task-uuid-1", 7L, "x", "1024x1024", 1, "medium", "done", null, 0, null, null, null,
                null, List.of(), List.of()));

        var row = jdbc.queryForMap("SELECT id, client_task_id, created_by FROM gallery.generation WHERE user_id = 7");
        assertThat(((Number) row.get("id")).longValue()).isGreaterThan(1_000_000_000L); // 内部雪花代理键
        assertThat(row.get("client_task_id")).isEqualTo("task-uuid-1"); // 对外标识不变
        assertThat(row.get("created_by")).isNull(); // 无请求上下文 → auditor empty
        assertThat(generations.findCreatedAt("task-uuid-1", 7L)).isPresent(); // 端口语义仍按任务 uuid
    }

    @Test
    void generationListFallsBackToFirstImageWhenNoThumb() {
        generations.save(new GenerationRepository.SaveGeneration(
                "g2", 7L, "x", "1024x1024", 1, "medium", "done", null, 0, null, null, null,
                null, // thumbKey null → 列表回退首图
                List.of(new GenerationRepository.OutputImage(0, "gen/7/g2/0.png", 1)), List.of()));
        assertThat(generations.list(7L, 1, 24).rows().get(0).thumbKey()).isEqualTo("gen/7/g2/0.png");
    }
}
