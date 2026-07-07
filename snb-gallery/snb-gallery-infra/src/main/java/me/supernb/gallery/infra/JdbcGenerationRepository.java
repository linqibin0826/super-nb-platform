package me.supernb.gallery.infra;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.gallery.app.GenerationRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// GenerationRepository 实现:生成历史 4 表落库/查库/回收键。落库在一个事务内。
@Repository
public class JdbcGenerationRepository implements GenerationRepository {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate txTemplate;

    public JdbcGenerationRepository(JdbcTemplate jdbcTemplate, PlatformTransactionManager txManager) {
        this.jdbc = jdbcTemplate;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public Optional<Instant> findCreatedAt(String id, long userId) {
        List<Timestamp> rows = jdbc.query(
                "SELECT created_at FROM gallery.generation WHERE id = ? AND user_id = ?",
                (rs, i) -> rs.getTimestamp("created_at"), id, userId);
        return rows.stream().findFirst().map(Timestamp::toInstant);
    }

    @Override
    public boolean refExists(long userId, String sha256) {
        Integer one = jdbc.query(
                "SELECT 1 FROM gallery.ref_image WHERE user_id = ? AND sha256 = ? LIMIT 1",
                (rs, i) -> 1, userId, sha256).stream().findFirst().orElse(null);
        return one != null;
    }

    @Override
    public Instant save(SaveGeneration c) {
        txTemplate.executeWithoutResult(status -> {
            jdbc.update(
                    "INSERT INTO gallery.generation "
                            + "(id, user_id, prompt, size, n, quality, status, cost, elapsed_ms, group_name, key_id, error, thumb_key) "
                            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (id) DO NOTHING",
                    c.id(), c.userId(), c.prompt(), c.size(), c.n(), c.quality(), c.status(),
                    c.cost(), c.elapsedMs(), c.groupName(), c.keyId(), c.error(), c.thumbKey());
            for (OutputImage o : c.outputs()) {
                jdbc.update(
                        "INSERT INTO gallery.generation_image (generation_id, idx, r2_key, bytes) "
                                + "VALUES (?,?,?,?) ON CONFLICT (generation_id, idx) DO NOTHING",
                        c.id(), o.idx(), o.r2Key(), o.bytes());
            }
            for (RefImage r : c.refs()) {
                jdbc.update(
                        "INSERT INTO gallery.ref_image (user_id, sha256, r2_key, bytes) "
                                + "VALUES (?,?,?,?) ON CONFLICT (user_id, sha256) DO NOTHING",
                        c.userId(), r.sha256(), r.r2Key(), r.bytes());
                jdbc.update(
                        "INSERT INTO gallery.generation_ref (generation_id, sha256, idx) "
                                + "VALUES (?,?,?) ON CONFLICT (generation_id, idx) DO NOTHING",
                        c.id(), r.sha256(), r.idx());
            }
        });
        Timestamp created = jdbc.queryForObject(
                "SELECT created_at FROM gallery.generation WHERE id = ?", Timestamp.class, c.id());
        return created.toInstant();
    }

    @Override
    public PageRows list(long userId, int page, int pageSize) {
        Long total = jdbc.queryForObject(
                "SELECT count(*) FROM gallery.generation WHERE user_id = ?", Long.class, userId);
        RowMapper<ListRow> mapper = (rs, i) -> new ListRow(
                rs.getString("id"), rs.getTimestamp("created_at").toInstant(), rs.getString("prompt"),
                rs.getString("size"), rs.getInt("n"), rs.getString("quality"), rs.getString("status"),
                (Double) rs.getObject("cost"), rs.getInt("elapsed_ms"), rs.getString("error"),
                rs.getString("thumb_key"));
        List<ListRow> rows = jdbc.query(
                "SELECT id, created_at, prompt, size, n, quality, status, cost, elapsed_ms, error, "
                        + "COALESCE(thumb_key, (SELECT r2_key FROM gallery.generation_image gi "
                        + "WHERE gi.generation_id = g.id ORDER BY idx LIMIT 1)) AS thumb_key "
                        + "FROM gallery.generation g WHERE user_id = ? "
                        + "ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                mapper, userId, pageSize, (long) (page - 1) * pageSize);
        return new PageRows(rows, total == null ? 0 : total);
    }

    @Override
    public Optional<DetailRow> detail(String id, long userId) {
        List<Object[]> head = jdbc.query(
                "SELECT created_at, prompt, size, n, quality, status, cost, elapsed_ms, group_name, key_id, error "
                        + "FROM gallery.generation WHERE id = ? AND user_id = ?",
                (rs, i) -> new Object[] {
                        rs.getTimestamp("created_at").toInstant(), rs.getString("prompt"), rs.getString("size"),
                        rs.getInt("n"), rs.getString("quality"), rs.getString("status"), (Double) rs.getObject("cost"),
                        rs.getInt("elapsed_ms"), rs.getString("group_name"), (Long) rs.getObject("key_id"),
                        rs.getString("error")},
                id, userId);
        if (head.isEmpty()) {
            return Optional.empty();
        }
        Object[] h = head.get(0);
        List<String> outputKeys = jdbc.query(
                "SELECT r2_key FROM gallery.generation_image WHERE generation_id = ? ORDER BY idx",
                (rs, i) -> rs.getString(1), id);
        List<String> refKeys = jdbc.query(
                "SELECT ri.r2_key FROM gallery.generation_ref gr "
                        + "JOIN gallery.ref_image ri ON ri.user_id = ? AND ri.sha256 = gr.sha256 "
                        + "WHERE gr.generation_id = ? ORDER BY gr.idx",
                (rs, i) -> rs.getString(1), userId, id);
        return Optional.of(new DetailRow(
                id, (Instant) h[0], (String) h[1], (String) h[2], (int) h[3], (String) h[4], (String) h[5],
                (Double) h[6], (int) h[7], (String) h[8], (Long) h[9], (String) h[10], outputKeys, refKeys));
    }

    @Override
    public Optional<List<String>> deleteReturningObjectKeys(String id, long userId) {
        List<String> thumb = jdbc.query(
                "SELECT thumb_key FROM gallery.generation WHERE id = ? AND user_id = ?",
                (rs, i) -> rs.getString("thumb_key"), id, userId);
        if (thumb.isEmpty()) {
            return Optional.empty();
        }
        List<String> keys = new java.util.ArrayList<>(jdbc.query(
                "SELECT r2_key FROM gallery.generation_image WHERE generation_id = ?",
                (rs, i) -> rs.getString(1), id));
        if (thumb.get(0) != null) {
            keys.add(thumb.get(0));
        }
        jdbc.update("DELETE FROM gallery.generation WHERE id = ?", id);
        return Optional.of(keys);
    }
}
