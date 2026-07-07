package me.supernb.gallery.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import me.supernb.gallery.app.GalleryDto;
import me.supernb.gallery.app.GenerationRepository;
import me.supernb.gallery.domain.SortMode;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// gallery 三仓储对真实 Flyway schema(Testcontainers PG)的集成测试。
@Testcontainers
class GalleryRepositoriesTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static JdbcPromptRepository prompts;
    static JdbcInteractionRepository interactions;
    static JdbcGenerationRepository generations;
    static long p1;
    static long p2;
    static long p3;

    @BeforeAll
    static void init() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds).schemas("gallery")
                .locations("classpath:db/migration/gallery").load().migrate();
        jdbc = new JdbcTemplate(ds);
        prompts = new JdbcPromptRepository(jdbc);
        interactions = new JdbcInteractionRepository(jdbc);
        generations = new JdbcGenerationRepository(jdbc, new DataSourceTransactionManager(ds));

        jdbc.update("INSERT INTO gallery.category (slug, axis, name_en, name_zh) VALUES "
                + "('portrait','scene','Portrait','人像'),('anime','style','Anime','动漫'),('animal','subject','Animal','动物')");
        p1 = insertPrompt("s1", "a cat", "portrait");
        p2 = insertPrompt("s2", "a dog", "anime");
        p3 = insertPrompt("s3", "sunset", null);
    }

    static long insertPrompt(String sourceId, String title, String categorySlug) {
        Integer catId = categorySlug == null ? null
                : jdbc.queryForObject("SELECT id FROM gallery.category WHERE slug = ?", Integer.class, categorySlug);
        return jdbc.queryForObject(
                "INSERT INTO gallery.prompt (source, source_id, title, prompt_text, author_name, category_id, image_w, image_h) "
                        + "VALUES ('own', ?, ?, 'PROMPT', 'alice', ?, 512, 512) RETURNING id",
                Long.class, sourceId, title, catId);
    }

    @BeforeEach
    void resetMutableState() {
        jdbc.update("TRUNCATE gallery.prompt_like, gallery.prompt_favorite");
        jdbc.update("TRUNCATE gallery.generation CASCADE");
        jdbc.update("TRUNCATE gallery.ref_image");
        jdbc.update("UPDATE gallery.prompt SET like_count = 0, fav_count = 0");
    }

    @Test
    void listReturnsPublishedPaged() {
        GalleryDto.Page<GalleryDto.PromptSummary> page = prompts.list(null, null, SortMode.FEATURED, 1, 24);
        assertThat(page.total()).isEqualTo(3);
        assertThat(page.items()).hasSize(3);
        assertThat(page.items().get(0).id()).isEqualTo(p3); // featured = id DESC
    }

    @Test
    void listFiltersByCategoryAndSearch() {
        assertThat(prompts.list("portrait", null, SortMode.FEATURED, 1, 24).total()).isEqualTo(1);
        assertThat(prompts.list(null, "cat", SortMode.FEATURED, 1, 24).items().get(0).id()).isEqualTo(p1);
    }

    @Test
    void detailReturnsFullWithCategoryOrEmpty() {
        GalleryDto.PromptDetail d = prompts.detail(p1).orElseThrow();
        assertThat(d.title()).isEqualTo("a cat");
        assertThat(d.category().slug()).isEqualTo("portrait");
        assertThat(prompts.detail(9999L)).isEmpty();
    }

    @Test
    void categoriesTreeHasCountsPerAxis() {
        GalleryDto.CategoryTree tree = prompts.categories();
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

        GalleryDto.Page<GalleryDto.PromptSummary> favs = interactions.myFavorites(7L, 1, 24);
        assertThat(favs.total()).isEqualTo(1);
        assertThat(favs.items().get(0).id()).isEqualTo(p1);

        GalleryDto.MyInteractions mine = interactions.myInteractions(List.of(p1, p2, p3), 7L);
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
    void generationListFallsBackToFirstImageWhenNoThumb() {
        generations.save(new GenerationRepository.SaveGeneration(
                "g2", 7L, "x", "1024x1024", 1, "medium", "done", null, 0, null, null, null,
                null, // thumbKey null → 列表回退首图
                List.of(new GenerationRepository.OutputImage(0, "gen/7/g2/0.png", 1)), List.of()));
        assertThat(generations.list(7L, 1, 24).rows().get(0).thumbKey()).isEqualTo("gen/7/g2/0.png");
    }
}
