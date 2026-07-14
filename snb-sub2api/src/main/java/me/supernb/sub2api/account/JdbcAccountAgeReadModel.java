package me.supernb.sub2api.account;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/// [AccountAgeReadModel] 的 JdbcTemplate 实现,经独立只读 DataSource 查 sub2api 库。
public class JdbcAccountAgeReadModel implements AccountAgeReadModel {

    private final JdbcTemplate jdbc;

    public JdbcAccountAgeReadModel(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> registeredOn(LocalDate localDate, ZoneId zone) {
        Instant start = localDate.atStartOfDay(zone).toInstant();
        Instant end = localDate.plusDays(1).atStartOfDay(zone).toInstant();
        return jdbc.query("SELECT id FROM users WHERE created_at >= ? AND created_at < ?",
                (rs, i) -> rs.getLong("id"), Timestamp.from(start), Timestamp.from(end));
    }
}
