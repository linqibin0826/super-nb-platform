package me.supernb.sub2api.referral;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import me.supernb.sub2api.EmailMask;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/// [ReferralReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
///
/// 充值榜:窗口内注册(users.created_at ∈ [start,end))且有 inviter 的新用户,其窗口内
/// `order_type='balance' AND status='COMPLETED' AND completed_at ∈ [start,end)` 的充值按邀请人
/// SUM,原始总额降序,capped=min(total,cap)。人数榜:曾开通新人组(user_subscriptions.group_id=gid,
/// 不滤 deleted_at)的被邀请人数按邀请人 COUNT DISTINCT。两榜均排除 role<>'user'、软删、inviter_id=1。
public class JdbcReferralReadModel implements ReferralReadModel {

    private final NamedParameterJdbcTemplate jdbc;

    /// 构造:注入指向 sub2api 只读源的 JdbcTemplate(包成 NamedParameterJdbcTemplate,启用具名参数)。
    public JdbcReferralReadModel(JdbcTemplate jdbcTemplate) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /// 充值榜:按邀请人聚合窗口内被邀请新用户的 COMPLETED 余额充值,原始总额降序取前 limit;
    /// name 经 `mask` 脱敏,capped=min(total,cap)。
    @Override
    public List<RechargeRow> rechargeBoard(Instant start, Instant end, BigDecimal cap, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end))
                .addValue("limit", limit);
        return jdbc.query(
                "SELECT iu.email AS inviter_email, SUM(po.amount) AS total "
                        + "FROM user_affiliates ua "
                        + "JOIN users iu ON iu.id = ua.inviter_id AND iu.role = 'user' AND iu.deleted_at IS NULL "
                        + "JOIN users u ON u.id = ua.user_id AND u.deleted_at IS NULL "
                        + "JOIN payment_orders po ON po.user_id = u.id "
                        + "WHERE ua.inviter_id IS NOT NULL AND ua.inviter_id <> 1 "
                        + "AND u.created_at >= :start AND u.created_at < :end "
                        + "AND po.order_type = 'balance' AND po.status = 'COMPLETED' "
                        + "AND po.completed_at >= :start AND po.completed_at < :end "
                        + "GROUP BY ua.inviter_id, iu.email ORDER BY total DESC LIMIT :limit",
                p,
                (rs, i) -> {
                    BigDecimal total = rs.getBigDecimal("total");
                    BigDecimal capped = total.min(cap);
                    return new RechargeRow(mask(rs.getString("inviter_email")), total, capped);
                });
    }

    /// 人数榜:曾开通新人组(不滤 deleted_at)的被邀请人数按邀请人聚合,人数降序取前 limit;name 经 `mask` 脱敏。
    @Override
    public List<InviteRow> inviteBoard(long newcomerGroupId, int limit) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("gid", newcomerGroupId)
                .addValue("limit", limit);
        return jdbc.query(
                "SELECT iu.email AS inviter_email, COUNT(DISTINCT ua.user_id) AS cnt "
                        + "FROM user_subscriptions us "
                        + "JOIN user_affiliates ua ON ua.user_id = us.user_id "
                        + "JOIN users u ON u.id = ua.user_id AND u.deleted_at IS NULL "
                        + "JOIN users iu ON iu.id = ua.inviter_id AND iu.role = 'user' AND iu.deleted_at IS NULL "
                        + "WHERE us.group_id = :gid AND ua.inviter_id IS NOT NULL AND ua.inviter_id <> 1 "
                        + "GROUP BY ua.inviter_id, iu.email ORDER BY cnt DESC LIMIT :limit",
                p,
                (rs, i) -> new InviteRow(mask(rs.getString("inviter_email")), rs.getInt("cnt")));
    }

    /// 新人总数:窗口内注册且未软删的用户数(只看注册,不看开组/邀请关系)。
    @Override
    public int newcomerTotal(Instant start, Instant end) {
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("start", Timestamp.from(start))
                .addValue("end", Timestamp.from(end));
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users u "
                        + "WHERE u.created_at >= :start AND u.created_at < :end AND u.deleted_at IS NULL",
                p, Integer.class);
        return n == null ? 0 : n;
    }

    /// 邮箱脱敏:委托全站唯一口径 [EmailMask#mask](恒 ≥2 位被遮,短本地名不再回显完整本地部分)。
    /// null 原样返回;未脱敏邮箱只在本方法作用域内出现。
    static String mask(String email) {
        return EmailMask.mask(email);
    }
}
