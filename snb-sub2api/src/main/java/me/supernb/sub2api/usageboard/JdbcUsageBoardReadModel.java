package me.supernb.sub2api.usageboard;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [UsageBoardReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
public class JdbcUsageBoardReadModel implements UsageBoardReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    /// 构造:注入指向 sub2api 只读源的 JdbcTemplate(包成 NamedParameterJdbcTemplate)。
    public JdbcUsageBoardReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public List<UsageRow> aggregate(Instant start, Instant end) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end));
        return jdbc.query(
                "SELECT u.id AS user_id, u.email, u.username, NULLIF(av.url, '') AS avatar_url, "
                        + "COALESCE(SUM(l.input_tokens + l.output_tokens + l.cache_creation_tokens + l.cache_read_tokens), 0) AS tokens, "
                        + "COUNT(*) AS requests, COALESCE(SUM(l.actual_cost), 0) AS cost "
                        + "FROM usage_logs l "
                        + "JOIN users u ON u.id = l.user_id "
                        + "AND u.role = 'user' AND u.status = 'active' AND u.deleted_at IS NULL "
                        + "LEFT JOIN user_avatars av ON av.user_id = u.id "
                        + "WHERE l.created_at >= :start AND l.created_at < :end "
                        + "AND EXISTS (SELECT 1 FROM payment_orders po WHERE po.user_id = u.id "
                        + "AND po.order_type = 'balance' AND po.status = 'COMPLETED') "
                        + "GROUP BY u.id, u.email, u.username, av.url",
                p,
                (rs, i) -> new UsageRow(
                        rs.getLong("user_id"),
                        displayName(rs.getString("username"), rs.getString("email")),
                        rs.getString("avatar_url"),
                        rs.getLong("tokens"),
                        rs.getLong("requests"),
                        rs.getDouble("cost")));
    }

    @Override
    public boolean eligible(long userId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_orders po WHERE po.user_id = :uid "
                        + "AND po.order_type = 'balance' AND po.status = 'COMPLETED'",
                new MapSqlParameterSource().addValue("uid", userId), Integer.class);
        return n != null && n > 0;
    }

    /// 择名:username 非空白用 username,否则脱敏邮箱。
    static String displayName(String username, String email) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return mask(email);
    }

    /// 邮箱脱敏:与 referral 榜同一契约——本地部分前 2 位 + `***` + 后 2 位(仅本地段 ≥5 位时
    /// 保留后缀) + @域名。改口径须与 JdbcReferralReadModel.mask 同步。null 原样返回。
    static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        String local = at >= 0 ? email.substring(0, at) : email;
        String domain = at >= 0 ? email.substring(at) : "";
        String prefix = local.length() >= 2 ? local.substring(0, 2) : local;
        String suffix = local.length() >= 5 ? local.substring(local.length() - 2) : "";
        return prefix + "***" + suffix + domain;
    }
}
