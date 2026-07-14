package me.supernb.activity.infra.adapter.read;

import java.util.List;
import me.supernb.activity.domain.port.read.LeaderboardAchievementSignalPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// LeaderboardAchievementSignalPort 实现:读既有 leaderboard_rank_snapshot 表取 MIN 名次。
@Component
public class LeaderboardAchievementSignalAdapter implements LeaderboardAchievementSignalPort {

    private final JdbcTemplate jdbc;

    public LeaderboardAchievementSignalAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<UserRank> bestRankEver() {
        return jdbc.query(
                "SELECT user_id, MIN(rank) AS best FROM activity.leaderboard_rank_snapshot GROUP BY user_id",
                (rs, i) -> new UserRank(rs.getLong("user_id"), rs.getInt("best")));
    }
}
