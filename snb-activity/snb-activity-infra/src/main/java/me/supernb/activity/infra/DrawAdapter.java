package me.supernb.activity.infra;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.DrawPort;
import me.supernb.activity.app.RechargeQueryPort;
import me.supernb.activity.domain.Campaign;
import me.supernb.activity.domain.DrawEligibility;
import me.supernb.activity.domain.DrawResult;
import me.supernb.activity.domain.NoDrawsLeftException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// DrawPort 实现:抽奖的原子事务在此。
///
/// 移植 activity-svc draw.py 的并发安全语义:事务内先 pg_advisory_xact_lock(userId) 串行化该用户,
/// 再现查剩余次数(充值总额走只读端口,弱一致可接受),然后 FOR UPDATE SKIP LOCKED 原子领槽;
/// 池空则记 $5 安慰奖占位(不发码)。用 TransactionTemplate 而非 @Transactional 注解,便于脱离 Spring 容器测试。
@Repository
public class DrawAdapter implements DrawPort {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate txTemplate;
    private final RechargeQueryPort rechargePort;

    public DrawAdapter(JdbcTemplate jdbcTemplate, PlatformTransactionManager txManager, RechargeQueryPort rechargePort) {
        this.jdbcTemplate = jdbcTemplate;
        this.txTemplate = new TransactionTemplate(txManager);
        this.rechargePort = rechargePort;
    }

    @Override
    public DrawResult drawFor(Campaign campaign, long userId) {
        return txTemplate.execute(status -> doDraw(campaign, userId));
    }

    private DrawResult doDraw(Campaign campaign, long userId) {
        // 事务级 advisory lock:随事务结束自动释放,防并发超额
        jdbcTemplate.queryForList("SELECT pg_advisory_xact_lock(?)", userId);

        BigDecimal total = rechargePort.totalRecharge(userId, campaign.startsAt(), campaign.endsAt());
        int used = countDraws(campaign.id(), userId);
        if (DrawEligibility.remainingDraws(total, used) <= 0) {
            throw new NoDrawsLeftException();
        }

        List<Map<String, Object>> claimed = jdbcTemplate.queryForList(
                "UPDATE activity.prize_slot SET status = 'claimed', claimed_by = ?, claimed_at = now() "
                        + "WHERE id = (SELECT id FROM activity.prize_slot "
                        + "WHERE campaign_id = ? AND status = 'available' "
                        + "ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED) "
                        + "RETURNING id, amount, redeem_code",
                userId, campaign.id());

        if (!claimed.isEmpty()) {
            Map<String, Object> row = claimed.get(0);
            long slotId = ((Number) row.get("id")).longValue();
            BigDecimal amount = (BigDecimal) row.get("amount");
            String code = (String) row.get("redeem_code");
            jdbcTemplate.update(
                    "INSERT INTO activity.draw (campaign_id, user_id, slot_id, amount, redeem_code, is_consolation) "
                            + "VALUES (?, ?, ?, ?, ?, false)",
                    campaign.id(), userId, slotId, amount, code);
            return DrawResult.prize(amount, code);
        }

        // 池空 → 安慰奖占位(不调 admin、不发码,人工发放)
        BigDecimal consolation = campaign.consolationAmount();
        jdbcTemplate.update(
                "INSERT INTO activity.draw (campaign_id, user_id, slot_id, amount, redeem_code, is_consolation) "
                        + "VALUES (?, ?, NULL, ?, NULL, true)",
                campaign.id(), userId, consolation);
        return DrawResult.consolation(consolation);
    }

    @Override
    public int countDraws(long campaignId, long userId) {
        Integer n = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activity.draw WHERE campaign_id = ? AND user_id = ?",
                Integer.class, campaignId, userId);
        return n == null ? 0 : n;
    }

    @Override
    public List<ActivityDto.RawDraw> myRawDraws(long campaignId, long userId) {
        return jdbcTemplate.query(
                "SELECT amount, redeem_code, is_consolation, created_at FROM activity.draw "
                        + "WHERE campaign_id = ? AND user_id = ? ORDER BY created_at DESC LIMIT 100",
                (rs, i) -> new ActivityDto.RawDraw(
                        rs.getBigDecimal("amount"),
                        rs.getString("redeem_code"),
                        rs.getBoolean("is_consolation"),
                        rs.getTimestamp("created_at").toInstant()),
                campaignId, userId);
    }

    @Override
    public List<ActivityDto.RawWinner> recentRealWinners(long campaignId, int limit) {
        return jdbcTemplate.query(
                "SELECT user_id, amount FROM activity.draw "
                        + "WHERE campaign_id = ? AND is_consolation = false ORDER BY created_at DESC LIMIT ?",
                (rs, i) -> new ActivityDto.RawWinner(rs.getLong("user_id"), rs.getBigDecimal("amount")),
                campaignId, limit);
    }
}
