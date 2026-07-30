package me.supernb.sub2api.recharge;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.supernb.sub2api.EmailMask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [RechargeReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
///
/// 窗口口径与前身 activity-svc 完全一致:`order_type='balance' AND status='COMPLETED'
/// AND completed_at ∈ [start, end)`;榜单/流水统计仅计入 role='user',流水额外滤掉 <¥10 的测试单。
public class JdbcRechargeReadModel implements RechargeReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    /// 构造:注入指向 sub2api 只读源的 JdbcTemplate(内部包成 NamedParameterJdbcTemplate,启用具名参数绑定)。
    public JdbcRechargeReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /// 窗口内该用户 COMPLETED 余额充值合计;无记录返回 0。
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

    /// 按用户聚合窗口内 COMPLETED 余额单、金额倒序取前 limit(仅 role=user),name 经 `mask` 脱敏。
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

    /// 按完成时间倒序取窗口内 COMPLETED 余额单前 limit(仅 role=user、金额 ≥¥10 滤测试单),name 经 `mask` 脱敏。
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

    /// 空 ids 直接短路空 map(不发 SQL);否则按 id 查 role=user 的用户,取展示名:
    /// 用户名非空白就用用户名,否则回退 `mask(email)`。
    ///
    /// 判空必须是 `isBlank` 不是 `!= null`:库里 username 既可能是 NULL 也可能是空串/纯空格
    /// (老号迁移留下的),只判 null 会让这些人在公开信息流里显示成一个空白名字。
    @Override
    public Map<Long, String> displayNamesByIds(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new HashMap<>();
        jdbc.query(
                "SELECT id, username, email FROM users WHERE id IN (:ids) AND role = 'user'",
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    String username = rs.getString("username");
                    result.put(rs.getLong("id"),
                            username == null || username.isBlank()
                                    ? mask(rs.getString("email"))
                                    : username.trim());
                });
        return result;
    }

    /// 空 codes 直接短路空 map;否则查 redeem_codes,expires_at 为空(NULL)时映射为 null。
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

    /// 窗口内有新增 COMPLETED 余额充值的用户 id(候选发现,payment_orders 窄扫描)。
    @Override
    public List<Long> usersWithNewRechargeSince(Instant since, Instant until) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("since", Timestamp.from(since))
                .addValue("until", Timestamp.from(until));
        return jdbc.query(
                "SELECT DISTINCT user_id FROM payment_orders WHERE order_type = 'balance' "
                        + "AND status = 'COMPLETED' AND completed_at >= :since AND completed_at < :until",
                p, (rs, i) -> rs.getLong("user_id"));
    }

    /// 疯四桶资格名单:先按 user_id 聚合出「窗口内单笔达标的首笔到账时刻」,再按该时刻升序取前 limit。
    /// 并列时以 user_id 兜底排序,保证同一窗口反复查桶序恒定(否则用户刷新一次桶序就变了)。
    @Override
    public List<Long> qualifiedUserIdsInOrder(Instant start, Instant end, BigDecimal minAmount, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end))
                .addValue("min", minAmount)
                .addValue("limit", limit);
        return jdbc.query(
                "SELECT q.user_id FROM ("
                        + "  SELECT user_id, MIN(completed_at) AS first_at FROM payment_orders"
                        + "  WHERE order_type = 'balance' AND status = 'COMPLETED' AND amount >= :min"
                        + "    AND completed_at >= :start AND completed_at < :end"
                        + "  GROUP BY user_id"
                        + ") q ORDER BY q.first_at, q.user_id LIMIT :limit",
                p, (rs, i) -> rs.getLong("user_id"));
    }

    /// 该用户在该分组下有没有带此 notes 的订阅(含已过期——过期记录仍在表里,
    /// 这正是「这一场领过没」要的语义:领过就是领过,卡过期了也不能再领一张)。
    @Override
    public boolean hasSubscription(long userId, long groupId, String notes) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("gid", groupId)
                .addValue("notes", notes);
        Boolean found = jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM user_subscriptions "
                        + "WHERE user_id = :uid AND group_id = :gid AND notes = :notes)",
                p, Boolean.class);
        return Boolean.TRUE.equals(found);
    }

    /// 邮箱脱敏:委托全站唯一口径 [EmailMask#mask]。恒 ≥2 位被遮(短本地名不再回显完整本地部分)。
    /// 未脱敏的完整邮箱只在本方法作用域内出现过,不向外传播。
    static String mask(String email) {
        return EmailMask.mask(email);
    }
}
