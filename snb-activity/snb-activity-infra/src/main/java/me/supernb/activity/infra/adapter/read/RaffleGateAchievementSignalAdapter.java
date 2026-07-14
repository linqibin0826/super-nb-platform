package me.supernb.activity.infra.adapter.read;

import java.util.List;
import me.supernb.activity.domain.port.read.RaffleGateAchievementSignalPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// RaffleGateAchievementSignalPort 实现:本域 activity schema 原生 SQL。
@Component
public class RaffleGateAchievementSignalAdapter implements RaffleGateAchievementSignalPort {

    private final JdbcTemplate jdbc;

    public RaffleGateAchievementSignalAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UserCount> raffleEntryCounts() {
        return count("SELECT user_id, COUNT(*) AS cnt FROM activity.raffle_entry GROUP BY user_id");
    }

    @Override
    public List<UserCount> raffleWinCounts() {
        return count("SELECT winner_user_id AS user_id, COUNT(*) AS cnt FROM activity.raffle_prize "
                + "WHERE winner_user_id IS NOT NULL GROUP BY winner_user_id");
    }

    @Override
    public List<UserCount> raffleCompanionCounts() {
        return count(
                "SELECT e.user_id, COUNT(*) AS cnt FROM activity.raffle_entry e "
                        + "JOIN activity.raffle_campaign c ON c.id = e.campaign_id AND c.status = 'drawn' "
                        + "WHERE NOT EXISTS (SELECT 1 FROM activity.raffle_prize p "
                        + "WHERE p.campaign_id = e.campaign_id AND p.winner_user_id = e.user_id) "
                        + "GROUP BY e.user_id");
    }

    @Override
    public List<UserCount> gateWinCounts() {
        return count("SELECT user_id, COUNT(*) AS cnt FROM activity.gate_attempt WHERE won = true GROUP BY user_id");
    }

    @Override
    public List<UserCount> drawcardCounts() {
        return count("SELECT user_id, COUNT(*) AS cnt FROM activity.draw GROUP BY user_id");
    }

    private List<UserCount> count(String sql) {
        return jdbc.query(sql, (rs, i) -> new UserCount(rs.getLong("user_id"), rs.getLong("cnt")));
    }
}
