package me.supernb.sub2api.raffle;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [RaffleGateReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
///
/// 口径逐字对齐既有读模型:RECHARGE 抄 JdbcRechargeReadModel.totalRecharge
/// (order_type='balance' AND status='COMPLETED',completed_at 窗口);
/// SPEND 抄用量榜金额口径(billing_type = 0 的 actual_cost,created_at 窗口)。
public class JdbcRaffleGateReadModel implements RaffleGateReadModel {

    private static final String RECHARGE_SINGLE =
            "SELECT COALESCE(SUM(amount), 0) FROM payment_orders "
                    + "WHERE user_id = :uid AND order_type = 'balance' AND status = 'COMPLETED' "
                    + "AND completed_at >= :from AND completed_at < :to";
    private static final String RECHARGE_BATCH =
            "SELECT user_id, COALESCE(SUM(amount), 0) AS total FROM payment_orders "
                    + "WHERE user_id IN (:ids) AND order_type = 'balance' AND status = 'COMPLETED' "
                    + "AND completed_at >= :from AND completed_at < :to GROUP BY user_id";
    private static final String SPEND_SINGLE =
            "SELECT COALESCE(SUM(actual_cost), 0) FROM usage_logs "
                    + "WHERE user_id = :uid AND billing_type = 0 "
                    + "AND created_at >= :from AND created_at < :to";
    private static final String SPEND_BATCH =
            "SELECT user_id, COALESCE(SUM(actual_cost), 0) AS total FROM usage_logs "
                    + "WHERE user_id IN (:ids) AND billing_type = 0 "
                    + "AND created_at >= :from AND created_at < :to GROUP BY user_id";

    private final NamedParameterJdbcTemplate jdbc;

    /// 构造:注入指向 sub2api 只读源的 JdbcTemplate(包成 NamedParameterJdbcTemplate)。
    public JdbcRaffleGateReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public BigDecimal gateValue(long userId, String gateType, Instant from, Instant to) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
        BigDecimal v = jdbc.queryForObject(singleSql(gateType), p, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public Map<Long, BigDecimal> gateValues(Collection<Long> userIds, String gateType, Instant from, Instant to) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("ids", userIds)
                .addValue("from", Timestamp.from(from))
                .addValue("to", Timestamp.from(to));
        Map<Long, BigDecimal> result = new HashMap<>();
        jdbc.query(batchSql(gateType), p,
                rs -> {
                    result.put(rs.getLong("user_id"), rs.getBigDecimal("total"));
                });
        return result;
    }

    @Override
    public Map<Long, Instant> registeredAts(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Instant> result = new HashMap<>();
        jdbc.query("SELECT id, created_at FROM users WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", userIds),
                rs -> {
                    Timestamp at = rs.getTimestamp("created_at");
                    if (at != null) {
                        result.put(rs.getLong("id"), at.toInstant());
                    }
                });
        return result;
    }

    @Override
    public Map<Long, String> displayNamesByIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        jdbc.query("SELECT id, email, username FROM users WHERE id IN (:ids)",
                new MapSqlParameterSource("ids", userIds),
                rs -> {
                    result.put(rs.getLong("id"), displayName(rs.getString("username"), rs.getString("email")));
                });
        return result;
    }

    private static String singleSql(String gateType) {
        return switch (gateType) {
            case "RECHARGE" -> RECHARGE_SINGLE;
            case "SPEND" -> SPEND_SINGLE;
            default -> throw new IllegalArgumentException("unknown gateType: " + gateType);
        };
    }

    private static String batchSql(String gateType) {
        return switch (gateType) {
            case "RECHARGE" -> RECHARGE_BATCH;
            case "SPEND" -> SPEND_BATCH;
            default -> throw new IllegalArgumentException("unknown gateType: " + gateType);
        };
    }

    /// 择名:username 非空白用 username,否则脱敏邮箱。契约与 JdbcUsageBoardReadModel 一致,改必同步。
    static String displayName(String username, String email) {
        if (username != null && !username.isBlank()) {
            return username;
        }
        return mask(email);
    }

    /// 邮箱脱敏:本地段前 2 + *** + 后 2(仅本地段 ≥5 时保留后缀)+ @域名。
    /// 契约同 JdbcUsageBoardReadModel.mask / JdbcReferralReadModel.mask,改口径三处同步。
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
