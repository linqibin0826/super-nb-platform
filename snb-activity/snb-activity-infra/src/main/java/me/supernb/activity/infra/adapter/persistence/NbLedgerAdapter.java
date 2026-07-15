package me.supernb.activity.infra.adapter.persistence;

import me.supernb.activity.domain.port.nb.NbLedgerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/// NbLedgerPort 实现:单条聚合 SQL,COALESCE 兜零。
@Repository
public class NbLedgerAdapter implements NbLedgerPort {

    private final JdbcTemplate jdbc;

    /// 构造:注入 Boot 主数据源的 JdbcTemplate。
    public NbLedgerAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public int totalPoints(long userId) {
        Integer sum = jdbc.queryForObject(
                "SELECT COALESCE(SUM(points), 0) FROM activity.nb_ledger WHERE user_id = ?",
                Integer.class, userId);
        return sum == null ? 0 : sum;
    }
}
