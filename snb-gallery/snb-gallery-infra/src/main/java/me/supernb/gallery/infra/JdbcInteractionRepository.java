package me.supernb.gallery.infra;

import java.util.List;
import java.util.OptionalInt;
import me.supernb.gallery.app.GalleryDto;
import me.supernb.gallery.app.InteractionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/// InteractionRepository 实现:点赞/收藏。toggle 用一条 CTE 完成「成员变更 + 计数 ±1」,
/// 幂等、计数不为负、只认 published;目标不存在 → UPDATE 0 行 → 空(服务转 404)。
@Repository
public class JdbcInteractionRepository implements InteractionRepository {

    private static final String SUMMARY_COLS =
            "p.id, p.title, p.image_url, p.image_w, p.image_h, p.author_name, p.like_count, p.fav_count";

    private static final RowMapper<GalleryDto.PromptSummary> SUMMARY = (rs, i) -> new GalleryDto.PromptSummary(
            rs.getLong("id"), rs.getString("title"), rs.getString("image_url"),
            (Integer) rs.getObject("image_w"), (Integer) rs.getObject("image_h"),
            rs.getString("author_name"), rs.getInt("like_count"), rs.getInt("fav_count"));

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcInteractionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public OptionalInt toggleLike(long promptId, long userId, boolean on) {
        return toggle("gallery.prompt_like", "like_count", promptId, userId, on);
    }

    @Override
    public OptionalInt toggleFavorite(long promptId, long userId, boolean on) {
        return toggle("gallery.prompt_favorite", "fav_count", promptId, userId, on);
    }

    // table / countCol 为内部常量(非用户输入),无注入面。
    private OptionalInt toggle(String table, String countCol, long promptId, long userId, boolean on) {
        String change = on
                ? "INSERT INTO " + table + " (prompt_id, user_id) SELECT id, :uid FROM tgt "
                        + "ON CONFLICT DO NOTHING RETURNING 1"
                : "DELETE FROM " + table + " WHERE prompt_id = (SELECT id FROM tgt) AND user_id = :uid RETURNING 1";
        String op = on ? "+" : "-";
        String sql = "WITH tgt AS (SELECT id FROM gallery.prompt WHERE id = :pid AND status = 'published'), "
                + "chg AS (" + change + ") "
                + "UPDATE gallery.prompt SET " + countCol + " = " + countCol + " " + op
                + " (SELECT count(*) FROM chg) WHERE id = (SELECT id FROM tgt) RETURNING " + countCol;
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("pid", promptId).addValue("uid", userId);
        List<Integer> result = jdbc.query(sql, p, (rs, i) -> rs.getInt(1));
        return result.isEmpty() ? OptionalInt.empty() : OptionalInt.of(result.get(0));
    }

    @Override
    public GalleryDto.Page<GalleryDto.PromptSummary> myFavorites(long userId, int page, int pageSize) {
        String base = "FROM gallery.prompt_favorite f JOIN gallery.prompt p ON p.id = f.prompt_id "
                + "WHERE f.user_id = :uid AND p.status = 'published'";
        Long total = jdbc.queryForObject("SELECT count(*) " + base, new MapSqlParameterSource("uid", userId), Long.class);
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("uid", userId).addValue("size", pageSize).addValue("off", (long) (page - 1) * pageSize);
        List<GalleryDto.PromptSummary> rows = jdbc.query(
                "SELECT " + SUMMARY_COLS + " " + base + " ORDER BY f.created_at DESC, f.prompt_id DESC "
                        + "LIMIT :size OFFSET :off",
                p, SUMMARY);
        return GalleryDto.Page.of(rows, total == null ? 0 : total, page, pageSize);
    }

    @Override
    public GalleryDto.MyInteractions myInteractions(List<Long> promptIds, long userId) {
        if (promptIds.isEmpty()) {
            return new GalleryDto.MyInteractions(List.of(), List.of());
        }
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("uid", userId).addValue("ids", promptIds);
        List<Long> liked = jdbc.query(
                "SELECT prompt_id FROM gallery.prompt_like WHERE user_id = :uid AND prompt_id IN (:ids)",
                p, (rs, i) -> rs.getLong(1));
        List<Long> favorited = jdbc.query(
                "SELECT prompt_id FROM gallery.prompt_favorite WHERE user_id = :uid AND prompt_id IN (:ids)",
                p, (rs, i) -> rs.getLong(1));
        return new GalleryDto.MyInteractions(liked, favorited);
    }
}
