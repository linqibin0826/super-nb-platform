package me.supernb.activity.infra;

import java.util.Optional;
import me.supernb.activity.app.CampaignPort;
import me.supernb.activity.domain.Campaign;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/// CampaignPort 实现:查活动库(snb 库 activity schema)取当前进行中活动。
@Repository
public class CampaignAdapter implements CampaignPort {

    private final JdbcTemplate jdbcTemplate;

    public CampaignAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Campaign> activeCampaign() {
        try {
            Campaign c = jdbcTemplate.queryForObject(
                    "SELECT id, name, starts_at, ends_at, status, consolation_amount "
                            + "FROM activity.campaign WHERE status = 'active' ORDER BY id DESC LIMIT 1",
                    (rs, i) -> new Campaign(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getTimestamp("starts_at").toInstant(),
                            rs.getTimestamp("ends_at").toInstant(),
                            rs.getString("status"),
                            rs.getBigDecimal("consolation_amount")));
            return Optional.ofNullable(c);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
