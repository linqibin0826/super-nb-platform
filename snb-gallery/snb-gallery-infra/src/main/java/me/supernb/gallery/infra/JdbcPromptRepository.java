package me.supernb.gallery.infra;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.supernb.gallery.app.GalleryDto;
import me.supernb.gallery.app.PromptRepository;
import me.supernb.gallery.domain.SortMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/// PromptRepository 实现:gallery 库只读查询。
@Repository
public class JdbcPromptRepository implements PromptRepository {

    private static final String SUMMARY_COLS =
            "p.id, p.title, p.image_url, p.image_w, p.image_h, p.author_name, p.like_count, p.fav_count";

    private static final RowMapper<GalleryDto.PromptSummary> SUMMARY = (rs, i) -> new GalleryDto.PromptSummary(
            rs.getLong("id"), rs.getString("title"), rs.getString("image_url"),
            (Integer) rs.getObject("image_w"), (Integer) rs.getObject("image_h"),
            rs.getString("author_name"), rs.getInt("like_count"), rs.getInt("fav_count"));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcPromptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public GalleryDto.Page<GalleryDto.PromptSummary> list(
            String categorySlug, String q, SortMode sort, int page, int pageSize) {
        StringBuilder join = new StringBuilder();
        StringBuilder where = new StringBuilder("p.status = 'published'");
        MapSqlParameterSource p = new MapSqlParameterSource();
        if (categorySlug != null && !categorySlug.isBlank()) {
            join.append(" JOIN gallery.category c ON c.id = p.category_id");
            where.append(" AND c.slug = :cat");
            p.addValue("cat", categorySlug);
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (p.title ILIKE :q OR p.description ILIKE :q)");
            p.addValue("q", "%" + q + "%");
        }
        String base = "FROM gallery.prompt p" + join + " WHERE " + where;
        Long total = jdbc.queryForObject("SELECT count(*) " + base, p, Long.class);
        p.addValue("size", pageSize).addValue("off", (long) (page - 1) * pageSize);
        List<GalleryDto.PromptSummary> rows = jdbc.query(
                "SELECT " + SUMMARY_COLS + " " + base + " ORDER BY " + orderSql(sort) + " LIMIT :size OFFSET :off",
                p, SUMMARY);
        return GalleryDto.Page.of(rows, total == null ? 0 : total, page, pageSize);
    }

    private static String orderSql(SortMode sort) {
        return switch (sort) {
            case NEWEST -> "p.source_published_at DESC NULLS LAST, p.id DESC";
            case LIKES -> "p.like_count DESC, p.id DESC";
            case FAVORITES -> "p.fav_count DESC, p.id DESC";
            case FEATURED -> "p.id DESC";
        };
    }

    @Override
    public Optional<GalleryDto.PromptDetail> detail(long id) {
        List<GalleryDto.PromptDetail> rows = jdbc.query(
                "SELECT p.id, p.source, p.title, p.description, p.prompt_text, p.lang, "
                        + "p.author_name, p.author_link, p.source_link, p.image_url, p.image_w, p.image_h, "
                        + "p.like_count, p.fav_count, p.source_published_at, p.created_at, "
                        + "c.slug AS c_slug, c.axis AS c_axis, c.name_zh AS c_name_zh, c.name_en AS c_name_en "
                        + "FROM gallery.prompt p LEFT JOIN gallery.category c ON c.id = p.category_id "
                        + "WHERE p.id = :id AND p.status = 'published'",
                new MapSqlParameterSource("id", id),
                (rs, i) -> mapDetail(rs));
        return rows.stream().findFirst();
    }

    private static GalleryDto.PromptDetail mapDetail(ResultSet rs) throws SQLException {
        GalleryDto.Category category = rs.getString("c_slug") == null ? null : new GalleryDto.Category(
                rs.getString("c_slug"), rs.getString("c_axis"), rs.getString("c_name_zh"), rs.getString("c_name_en"));
        return new GalleryDto.PromptDetail(
                rs.getLong("id"), rs.getString("source"), rs.getString("title"), rs.getString("description"),
                rs.getString("prompt_text"), rs.getString("lang"), rs.getString("author_name"),
                rs.getString("author_link"), rs.getString("source_link"), rs.getString("image_url"),
                (Integer) rs.getObject("image_w"), (Integer) rs.getObject("image_h"),
                rs.getInt("like_count"), rs.getInt("fav_count"),
                toInstant(rs.getTimestamp("source_published_at")), toInstant(rs.getTimestamp("created_at")),
                category);
    }

    @Override
    public GalleryDto.CategoryTree categories() {
        List<GalleryDto.CategoryNode> scene = new ArrayList<>();
        List<GalleryDto.CategoryNode> style = new ArrayList<>();
        List<GalleryDto.CategoryNode> subject = new ArrayList<>();
        jdbc.query(
                "SELECT c.axis, c.slug, c.name_zh, c.name_en, "
                        + "(count(p.id) FILTER (WHERE p.status = 'published'))::int AS cnt "
                        + "FROM gallery.category c LEFT JOIN gallery.prompt p ON p.category_id = c.id "
                        + "GROUP BY c.id ORDER BY c.axis, c.sort, c.id",
                new MapSqlParameterSource(),
                (RowMapper<Void>) (rs, i) -> {
                    GalleryDto.CategoryNode node = new GalleryDto.CategoryNode(
                            rs.getString("slug"), rs.getString("name_zh"), rs.getString("name_en"), rs.getInt("cnt"));
                    switch (rs.getString("axis")) {
                        case "scene" -> scene.add(node);
                        case "style" -> style.add(node);
                        case "subject" -> subject.add(node);
                        default -> { }
                    }
                    return null;
                });
        return new GalleryDto.CategoryTree(scene, style, subject);
    }

    private static Instant toInstant(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }
}
