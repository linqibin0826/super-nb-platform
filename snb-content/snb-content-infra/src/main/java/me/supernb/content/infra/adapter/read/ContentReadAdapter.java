package me.supernb.content.infra.adapter.read;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.supernb.content.domain.model.read.ArticleDetail;
import me.supernb.content.domain.model.read.ArticleSummary;
import me.supernb.content.domain.model.read.CategoryView;
import me.supernb.content.domain.model.read.Page;
import me.supernb.content.domain.port.read.ContentReadPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/// [ContentReadPort] 实现：原生 SQL 只读查询（列表分页、slug 详情、分类可见计数）。
///
/// 过滤条件按需拼常量片段（照 PromptReadAdapter 的动态拼接纪律）；tag 过滤走
/// `tags @> jsonb_build_array(:tag)`，命中 GIN 索引。
@Repository
public class ContentReadAdapter implements ContentReadPort {

    private static final TypeReference<List<String>> TAGS_TYPE = new TypeReference<>() { };

    /// tags 列只是纯字符串数组，自持默认 Mapper（线程安全，只读用），不依赖应用级 Jackson 配置。
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SUMMARY_COLUMNS = """
            a.id, a.slug, a.type, a.title, a.summary, a.cover_url,
            a.category_slug, c.name AS category_name, a.tags::text AS tags_json,
            a.source_name, a.published_at
            """;

    private final JdbcClient jdbc;

    /// 构造：注入 JdbcClient。
    public ContentReadAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /// 可见文章分页；`page` 从 1 起（上游 Controller 已钳制）。
    @Override
    public Page<ArticleSummary> list(String categorySlug, String tag, int page, int pageSize) {
        boolean byCategory = categorySlug != null && !categorySlug.isBlank();
        boolean byTag = tag != null && !tag.isBlank();

        StringBuilder from = new StringBuilder("""
                FROM content.article a
                JOIN content.category c ON c.slug = a.category_slug
                WHERE NOT a.hidden
                """);
        if (byCategory) {
            from.append(" AND a.category_slug = :category");
        }
        if (byTag) {
            from.append(" AND a.tags @> jsonb_build_array(:tag)");
        }

        JdbcClient.StatementSpec count = jdbc.sql("SELECT count(*) " + from);
        JdbcClient.StatementSpec rows = jdbc.sql("SELECT " + SUMMARY_COLUMNS + from
                + " ORDER BY a.published_at DESC, a.id DESC LIMIT :limit OFFSET :offset")
                .param("limit", pageSize)
                .param("offset", (page - 1) * pageSize);
        if (byCategory) {
            count = count.param("category", categorySlug);
            rows = rows.param("category", categorySlug);
        }
        if (byTag) {
            count = count.param("tag", tag);
            rows = rows.param("tag", tag);
        }

        long total = count.query(Long.class).single();
        List<ArticleSummary> items = rows.query((rs, i) -> mapSummary(rs)).list();
        return Page.of(items, total, page, pageSize);
    }

    /// 可见文章详情（含正文/电子书路径/原文链接）。
    @Override
    public Optional<ArticleDetail> findVisibleBySlug(String slug) {
        return jdbc.sql("SELECT " + SUMMARY_COLUMNS + ", a.body_html, a.ebook_path, a.source_url "
                + "FROM content.article a JOIN content.category c ON c.slug = a.category_slug "
                + "WHERE NOT a.hidden AND a.slug = :slug")
                .param("slug", slug)
                .query((rs, i) -> new ArticleDetail(
                        String.valueOf(rs.getLong("id")), rs.getString("slug"), rs.getString("type"),
                        rs.getString("title"), rs.getString("summary"), rs.getString("cover_url"),
                        rs.getString("category_slug"), rs.getString("category_name"),
                        readTags(rs.getString("tags_json")), rs.getString("body_html"),
                        rs.getString("ebook_path"), rs.getString("source_name"), rs.getString("source_url"),
                        rs.getObject("published_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    /// 全部分类 + 可见文章计数（一次 GROUP BY，不逐分类查，避免 N+1）。
    @Override
    public List<CategoryView> categories() {
        return jdbc.sql("""
                SELECT c.slug, c.name, c.sort_order,
                       count(a.id) FILTER (WHERE NOT a.hidden) AS cnt
                FROM content.category c
                LEFT JOIN content.article a ON a.category_slug = c.slug
                GROUP BY c.slug, c.name, c.sort_order
                ORDER BY c.sort_order, c.slug
                """)
                .query((rs, i) -> new CategoryView(
                        rs.getString("slug"), rs.getString("name"), rs.getInt("sort_order"), rs.getLong("cnt")))
                .list();
    }

    /// 列表行 → 卡片视图（id String 化发生在此）。
    private ArticleSummary mapSummary(ResultSet rs) throws SQLException {
        return new ArticleSummary(
                String.valueOf(rs.getLong("id")), rs.getString("slug"), rs.getString("type"),
                rs.getString("title"), rs.getString("summary"), rs.getString("cover_url"),
                rs.getString("category_slug"), rs.getString("category_name"),
                readTags(rs.getString("tags_json")), rs.getString("source_name"),
                rs.getObject("published_at", OffsetDateTime.class).toInstant());
    }

    /// jsonb 数组列 → List<String>（列有 NOT NULL DEFAULT '[]'，null 仅防御）。
    private List<String> readTags(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(json, TAGS_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("tags 列不是合法 JSON 数组", e);
        }
    }
}
