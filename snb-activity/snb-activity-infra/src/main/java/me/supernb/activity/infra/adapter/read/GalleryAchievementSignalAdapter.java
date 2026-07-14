package me.supernb.activity.infra.adapter.read;

import java.util.List;
import me.supernb.activity.domain.port.read.GalleryAchievementSignalPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// GalleryAchievementSignalPort 实现:同库跨 gallery schema 原生 SQL,用 Boot 主数据源的
/// JdbcTemplate(gallery 的表与 activity 的表physically 同一个 snb 库,不同 schema 而已)。
@Component
public class GalleryAchievementSignalAdapter implements GalleryAchievementSignalPort {

    private final JdbcTemplate jdbc;

    public GalleryAchievementSignalAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UserCount> generationDoneCounts() {
        return jdbc.query("SELECT user_id, COUNT(*) AS cnt FROM gallery.generation "
                        + "WHERE status = 'done' GROUP BY user_id",
                (rs, i) -> new UserCount(rs.getLong("user_id"), rs.getLong("cnt")));
    }

    @Override
    public List<UserCount> likeAndFavoriteCounts() {
        return jdbc.query(
                "SELECT user_id, COUNT(*) AS cnt FROM ("
                        + "SELECT user_id FROM gallery.prompt_like "
                        + "UNION ALL SELECT user_id FROM gallery.prompt_favorite"
                        + ") x GROUP BY user_id",
                (rs, i) -> new UserCount(rs.getLong("user_id"), rs.getLong("cnt")));
    }
}
