package me.supernb.activity.infra.adapter.metric;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.domain.port.metric.UserMetricPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// UserMetricPort 实现:纯 JdbcTemplate 自然键 upsert(仿 JdbcRankSnapshotAdapter 惯例,
/// 不走 JPA 实体)。
@Component
public class JdbcUserMetricAdapter implements UserMetricPort {

    private final JdbcTemplate jdbc;

    /// 构造:注入 Boot 主数据源的 JdbcTemplate。
    public JdbcUserMetricAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(long userId, String metricCode, double value) {
        jdbc.update("INSERT INTO activity.user_metric (user_id, metric_code, value) VALUES (?, ?, ?) "
                        + "ON CONFLICT (user_id, metric_code) DO UPDATE SET value = EXCLUDED.value, updated_at = now()",
                userId, metricCode, value);
    }

    @Override
    public void upsertBatch(String metricCode, Map<Long, Double> values) {
        if (values.isEmpty()) {
            return;
        }
        List<Map.Entry<Long, Double>> entries = new ArrayList<>(values.entrySet());
        jdbc.batchUpdate(
                "INSERT INTO activity.user_metric (user_id, metric_code, value) VALUES (?, ?, ?) "
                        + "ON CONFLICT (user_id, metric_code) DO UPDATE SET value = EXCLUDED.value, updated_at = now()",
                entries, 500,
                (PreparedStatement ps, Map.Entry<Long, Double> e) -> {
                    ps.setLong(1, e.getKey());
                    ps.setString(2, metricCode);
                    ps.setDouble(3, e.getValue());
                });
    }

    @Override
    public Optional<Double> value(long userId, String metricCode) {
        List<Double> rows = jdbc.query(
                "SELECT value FROM activity.user_metric WHERE user_id = ? AND metric_code = ?",
                (rs, i) -> rs.getDouble("value"), userId, metricCode);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public Map<String, Double> allMetrics(long userId) {
        Map<String, Double> result = new HashMap<>();
        jdbc.query("SELECT metric_code, value FROM activity.user_metric WHERE user_id = ?",
                (rs) -> {
                    result.put(rs.getString("metric_code"), rs.getDouble("value"));
                }, userId);
        return result;
    }

    @Override
    public List<Long> usersUpdatedSince(Instant since) {
        return jdbc.query(
                "SELECT DISTINCT user_id FROM activity.user_metric WHERE updated_at > ?",
                (rs, i) -> rs.getLong("user_id"), Timestamp.from(since));
    }
}
