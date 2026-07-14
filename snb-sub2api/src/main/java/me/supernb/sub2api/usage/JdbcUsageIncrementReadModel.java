package me.supernb.sub2api.usage;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [UsageIncrementReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
public class JdbcUsageIncrementReadModel implements UsageIncrementReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcUsageIncrementReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Map<Long, Long> callCountsSince(Instant since, Instant until) {
        Map<Long, Long> result = new HashMap<>();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("since", Timestamp.from(since))
                .addValue("until", Timestamp.from(until));
        jdbc.query("SELECT user_id, COUNT(*) AS cnt FROM usage_logs "
                        + "WHERE created_at >= :since AND created_at < :until GROUP BY user_id",
                p, rs -> {
                    result.put(rs.getLong("user_id"), rs.getLong("cnt"));
                });
        return result;
    }

    @Override
    public Map<Long, Boolean> lateNightFlagsSince(Instant since, Instant until) {
        Map<Long, Boolean> result = new HashMap<>();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("since", Timestamp.from(since))
                .addValue("until", Timestamp.from(until));
        jdbc.query("SELECT DISTINCT user_id FROM usage_logs "
                        + "WHERE created_at >= :since AND created_at < :until "
                        + "AND EXTRACT(HOUR FROM created_at AT TIME ZONE 'Asia/Shanghai') BETWEEN 1 AND 4",
                p, rs -> {
                    result.put(rs.getLong("user_id"), true);
                });
        return result;
    }

    @Override
    public Map<Long, Long> callCountsOnDay(LocalDate day, ZoneId zone) {
        Instant start = day.atStartOfDay(zone).toInstant();
        Instant end = day.plusDays(1).atStartOfDay(zone).toInstant();
        Map<Long, Long> result = new HashMap<>();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end));
        jdbc.query("SELECT user_id, COUNT(*) AS cnt FROM usage_logs "
                        + "WHERE created_at >= :start AND created_at < :end GROUP BY user_id",
                p, rs -> {
                    result.put(rs.getLong("user_id"), rs.getLong("cnt"));
                });
        return result;
    }
}
