package me.supernb.sub2api.recharge;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// RechargeReadModel 的 JdbcTemplate 实现,查 sub2api 库(通过独立只读 DataSource)。
///
/// 窗口口径与 activity-svc 完全一致:order_type='balance' AND status='COMPLETED'
/// AND completed_at ∈ [start, end);榜单/流水仅 role='user',流水额外滤 <¥10 的测试单。
public class JdbcRechargeReadModel implements RechargeReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcRechargeReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public BigDecimal totalRecharge(long userId, Instant start, Instant end) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end));
        BigDecimal total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM payment_orders "
                        + "WHERE user_id = :uid AND order_type = 'balance' AND status = 'COMPLETED' "
                        + "AND completed_at >= :start AND completed_at < :end",
                p, BigDecimal.class);
        return total == null ? BigDecimal.ZERO : total;
    }

    @Override
    public List<LeaderRow> leaderboard(Instant start, Instant end, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end))
                .addValue("limit", limit);
        return jdbc.query(
                "SELECT u.email, SUM(po.amount) AS total FROM payment_orders po "
                        + "JOIN users u ON u.id = po.user_id "
                        + "WHERE po.order_type = 'balance' AND po.status = 'COMPLETED' "
                        + "AND po.completed_at >= :start AND po.completed_at < :end AND u.role = 'user' "
                        + "GROUP BY u.id, u.email ORDER BY total DESC LIMIT :limit",
                p,
                (rs, i) -> new LeaderRow(mask(rs.getString("email")), rs.getBigDecimal("total")));
    }

    @Override
    public List<RechargeRow> recentRecharges(Instant start, Instant end, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end))
                .addValue("limit", limit);
        return jdbc.query(
                "SELECT u.email, po.amount, po.completed_at FROM payment_orders po "
                        + "JOIN users u ON u.id = po.user_id "
                        + "WHERE po.order_type = 'balance' AND po.status = 'COMPLETED' "
                        + "AND po.completed_at >= :start AND po.completed_at < :end "
                        + "AND po.amount >= 10 AND u.role = 'user' "
                        + "ORDER BY po.completed_at DESC LIMIT :limit",
                p,
                (rs, i) -> new RechargeRow(
                        mask(rs.getString("email")),
                        rs.getBigDecimal("amount"),
                        rs.getTimestamp("completed_at").toInstant()));
    }

    @Override
    public Map<Long, String> maskedEmailsByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        jdbc.query(
                "SELECT id, email FROM users WHERE id IN (:ids) AND role = 'user'",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    result.put(rs.getLong("id"), mask(rs.getString("email")));
                });
        return result;
    }

    @Override
    public Map<String, RedeemCodeStatus> codeStatuses(Collection<String> codes) {
        if (codes.isEmpty()) {
            return Map.of();
        }
        Map<String, RedeemCodeStatus> result = new HashMap<>();
        jdbc.query(
                "SELECT code, status, expires_at FROM redeem_codes WHERE code IN (:codes)",
                new MapSqlParameterSource("codes", codes),
                rs -> {
                    Timestamp exp = rs.getTimestamp("expires_at");
                    result.put(rs.getString("code"),
                            new RedeemCodeStatus(rs.getString("status"), exp == null ? null : exp.toInstant()));
                });
        return result;
    }

    /// 邮箱脱敏:本地部分前 2 位 + *** + @域名(如 ab***@qq.com)。全名邮箱只在本方法内存在。
    static String mask(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        String local = at >= 0 ? email.substring(0, at) : email;
        String domain = at >= 0 ? email.substring(at) : "";
        String prefix = local.length() >= 2 ? local.substring(0, 2) : local;
        return prefix + "***" + domain;
    }
}
