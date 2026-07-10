package me.supernb.content.infra.adapter.persistence;

import me.supernb.content.domain.exception.ContentException;
import me.supernb.content.domain.port.repository.ArticleRepository;
import me.supernb.content.domain.port.repository.CategoryRepository;
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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// content 两个写适配器对真实 Flyway schema（Testcontainers PG）的集成测试。
@SpringBootTest(classes = ContentInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class ContentRepositoriesTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/content");
        r.add("spring.flyway.schemas", () -> "content");
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ArticleRepositoryAdapter articleRepository;

    @Autowired
    CategoryRepositoryAdapter categoryRepository;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE content.article, content.category");
    }

    /// 测试造数工厂：分类固定 tutorials，其余按参给定。
    ArticleRepository.ArticleData data(String slug, String title, Instant publishedAt, boolean hidden) {
        return new ArticleRepository.ArticleData(slug, "article", title, "s", null,
                "tutorials", List.of("Codex", "MCP"), "<p>hi</p>", null,
                null, null, publishedAt, hidden);
    }

    ArticleRepository.ArticleData data(String slug, String title) {
        return data(slug, title, Instant.parse("2026-07-10T00:00:00Z"), false);
    }

    @Test
    void upsertCreatesThenUpdatesBySlug() {
        categoryRepository.sync(List.of(new CategoryRepository.CategoryData("tutorials", "教程", 1)));

        var first = articleRepository.upsert(data("hello-codex", "第一版标题"));
        var second = articleRepository.upsert(data("hello-codex", "改稿后的标题"));

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.id()).isEqualTo(first.id()); // 幂等：同 slug 不换 id
        assertThat(first.id()).isGreaterThan(1_000_000_000L); // 雪花量级
        assertThat(jdbc.queryForObject("SELECT title FROM content.article WHERE slug = 'hello-codex'", String.class))
                .isEqualTo("改稿后的标题");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM content.article", Long.class)).isEqualTo(1L);
    }

    @Test
    void categorySyncRefusesDeletingReferencedCategory() {
        categoryRepository.sync(List.of(
                new CategoryRepository.CategoryData("tutorials", "教程", 1),
                new CategoryRepository.CategoryData("news", "资讯", 2)));
        articleRepository.upsert(data("hello-codex", "标题")); // 引用 tutorials

        assertThatThrownBy(() -> categoryRepository.sync(List.of(
                new CategoryRepository.CategoryData("news", "资讯", 1))))
                .isInstanceOf(ContentException.class);

        assertThat(categoryRepository.exists("tutorials")).isTrue(); // 拒删后事务回滚原样保留
        assertThat(categoryRepository.exists("news")).isTrue();
    }

    @Test
    void categorySyncUpsertsAndDeletesUnreferenced() {
        categoryRepository.sync(List.of(
                new CategoryRepository.CategoryData("tutorials", "教程", 1),
                new CategoryRepository.CategoryData("news", "资讯", 2)));

        var outcome = categoryRepository.sync(List.of(
                new CategoryRepository.CategoryData("tutorials", "教程（改名）", 1)));

        assertThat(outcome.deleted()).isEqualTo(1);
        assertThat(categoryRepository.exists("news")).isFalse();
        assertThat(jdbc.queryForObject("SELECT name FROM content.category WHERE slug = 'tutorials'", String.class))
                .isEqualTo("教程（改名）");
    }
}
