package me.supernb.activity.infra.adapter.snapshot;

import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.port.snapshot.RankSnapshotPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// RankSnapshotPort 的 JDBC 实现:快照表在 platform 自己的 snb 库 activity schema
/// (Boot 主数据源的 JdbcTemplate),不是 sub2api 只读库。同日重跑走 upsert 覆盖,天然幂等。
@Component
public class JdbcRankSnapshotAdapter implements RankSnapshotPort {

    private final JdbcTemplate jdbc;

    /// 构造:注入 Boot 主数据源的 JdbcTemplate。
    public JdbcRankSnapshotAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /// 批量 upsert 一组名次(500/批);空 map 直接返回,不产生空批次。
    @Override
    public void save(LocalDate date, BoardPeriod period, BoardMetric metric, Map<Long, Integer> ranks) {
        if (ranks.isEmpty()) {
            return;
        }
        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(ranks.entrySet());
        jdbc.batchUpdate(
                "INSERT INTO activity.leaderboard_rank_snapshot(snapshot_date, period, metric, user_id, rank) "
                        + "VALUES (?, ?, ?, ?, ?) "
                        + "ON CONFLICT (snapshot_date, period, metric, user_id) DO UPDATE SET rank = EXCLUDED.rank",
                entries,
                500,
                (PreparedStatement ps, Map.Entry<Long, Integer> e) -> {
                    ps.setObject(1, date);
                    ps.setString(2, code(period.name()));
                    ps.setString(3, code(metric.name()));
                    ps.setLong(4, e.getKey());
                    ps.setInt(5, e.getValue());
                });
    }

    /// 取 date 之前(严格早于)最近一次快照的名次映射;查无返回空 map。
    @Override
    public Map<Long, Integer> latestBefore(LocalDate date, BoardPeriod period, BoardMetric metric) {
        String p = code(period.name());
        String m = code(metric.name());
        Map<Long, Integer> out = new HashMap<>();
        jdbc.query(
                "SELECT user_id, rank FROM activity.leaderboard_rank_snapshot "
                        + "WHERE period = ? AND metric = ? AND snapshot_date = ("
                        + "SELECT MAX(snapshot_date) FROM activity.leaderboard_rank_snapshot "
                        + "WHERE period = ? AND metric = ? AND snapshot_date < ?)",
                rs -> {
                    out.put(rs.getLong("user_id"), rs.getInt("rank"));
                },
                p, m, p, m, date);
        return out;
    }

    /// 清理早于 cutoff 的历史快照。
    @Override
    public void purgeOlderThan(LocalDate cutoff) {
        jdbc.update("DELETE FROM activity.leaderboard_rank_snapshot WHERE snapshot_date < ?", cutoff);
    }

    /// 枚举名转表内小写码(day|week|month|all / tokens|amount)。
    private static String code(String enumName) {
        return enumName.toLowerCase(Locale.ROOT);
    }
}
