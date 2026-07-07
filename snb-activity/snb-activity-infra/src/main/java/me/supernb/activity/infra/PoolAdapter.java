package me.supernb.activity.infra;

import java.util.List;
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.PoolPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/// PoolPort 实现:按档位统计奖池余量。只出份数,绝不带出 redeem_code / claimed_by。
@Repository
public class PoolAdapter implements PoolPort {

    private final JdbcTemplate jdbcTemplate;

    public PoolAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ActivityDto.PoolTier> pool(long campaignId) {
        return jdbcTemplate.query(
                "SELECT amount, COUNT(*) AS total, COUNT(*) FILTER (WHERE status = 'available') AS available "
                        + "FROM activity.prize_slot WHERE campaign_id = ? GROUP BY amount ORDER BY amount",
                (rs, i) -> new ActivityDto.PoolTier(
                        rs.getBigDecimal("amount"), rs.getInt("total"), rs.getInt("available")),
                campaignId);
    }
}
