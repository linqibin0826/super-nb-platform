package me.supernb.sub2api.raffle;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import me.supernb.sub2api.EmailMask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [RaffleGateReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
///
/// 口径:RECHARGE=COMPLETED 余额单(completed_at 窗口)+已核销余额兑换码(type='balance'
/// AND status='used',used_at 窗口)——兑换码购码充值(闲鱼渠道)与在线支付同等计入
/// (2026-07-13 站长拍板补口径)。⚠️ ZPay 完成单会自动建同码 balance 兑换码并即时核销
/// (payment_fulfillment 镜像),按 payment_orders.recharge_code=code 关联剔除防双算;
/// 平台赠发的 balance 码(如 07-01 充值抽奖奖槽码)会计入,对 ¥50 门槛核算零增员,接受。
/// SPEND 抄用量榜金额口径(billing_type = 0 的 actual_cost,created_at 窗口)。
public class JdbcRaffleGateReadModel implements RaffleGateReadModel {

    private static final String REDEEM_NOT_ZPAY_MIRROR =
            "AND NOT EXISTS (SELECT 1 FROM payment_orders po WHERE po.recharge_code = rc.code)";
    private static final String RECHARGE_SINGLE =
            "SELECT COALESCE((SELECT SUM(amount) FROM payment_orders "
                    + "WHERE user_id = :uid AND order_type = 'balance' AND status = 'COMPLETED' "
                    + "AND completed_at >= :from AND completed_at < :to), 0) "
                    + "+ COALESCE((SELECT SUM(rc.value) FROM redeem_codes rc "
                    + "WHERE rc.used_by = :uid AND rc.type = 'balance' AND rc.status = 'used' "
                    + "AND rc.used_at >= :from AND rc.used_at < :to "
                    + REDEEM_NOT_ZPAY_MIRROR + "), 0)";
    private static final String RECHARGE_BATCH =
            "SELECT user_id, SUM(total) AS total FROM ("
                    + "SELECT user_id, SUM(amount) AS total FROM payment_orders "
                    + "WHERE user_id IN (:ids) AND order_type = 'balance' AND status = 'COMPLETED' "
                    + "AND completed_at >= :from AND completed_at < :to GROUP BY user_id "
                    + "UNION ALL "
                    + "SELECT rc.used_by AS user_id, SUM(rc.value) AS total FROM redeem_codes rc "
                    + "WHERE rc.used_by IN (:ids) AND rc.type = 'balance' AND rc.status = 'used' "
                    + "AND rc.used_at >= :from AND rc.used_at < :to "
                    + REDEEM_NOT_ZPAY_MIRROR
                    + " GROUP BY rc.used_by) t GROUP BY user_id";
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

    /// 邮箱脱敏:委托全站唯一口径 [EmailMask#mask](恒 ≥2 位被遮,短本地名不再回显完整本地部分)。null 原样返回。
    static String mask(String email) {
        return EmailMask.mask(email);
    }
}
