package me.supernb.activity.infra.adapter.read;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import me.supernb.activity.domain.port.read.CheckinMetricSignalPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/// CheckinMetricSignalPort 实现:本域 activity schema 原生 SQL,读既有 checkin_record 表。
@Component
public class CheckinMetricSignalAdapter implements CheckinMetricSignalPort {

    private final JdbcTemplate jdbc;

    public CheckinMetricSignalAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> usersCheckedInOn(LocalDate day) {
        return jdbc.query("SELECT DISTINCT user_id FROM activity.checkin_record WHERE checkin_date = ?",
                (rs, i) -> rs.getLong("user_id"), day);
    }

    @Override
    public List<Long> usersCheckedInAtMidnightOn(LocalDate day) {
        return jdbc.query(
                "SELECT DISTINCT user_id FROM activity.checkin_record WHERE checkin_date = ? "
                        + "AND (checked_in_at AT TIME ZONE 'Asia/Shanghai')::time < TIME '00:01:00'",
                (rs, i) -> rs.getLong("user_id"), day);
    }

    @Override
    public boolean hasGhostReturnAsOf(long userId, LocalDate asOf) {
        List<LocalDate> lastTwo = jdbc.query(
                "SELECT checkin_date FROM activity.checkin_record WHERE user_id = ? AND checkin_date <= ? "
                        + "ORDER BY checkin_date DESC LIMIT 2",
                (rs, i) -> rs.getObject("checkin_date", LocalDate.class), userId, asOf);
        if (lastTwo.size() < 2) {
            return false;
        }
        return ChronoUnit.DAYS.between(lastTwo.get(1), lastTwo.get(0)) >= 30;
    }

    @Override
    public List<Long> usersCheckedInBetween(LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT DISTINCT user_id FROM activity.checkin_record WHERE checkin_date BETWEEN ? AND ?",
                (rs, i) -> rs.getLong("user_id"), from, to);
    }
}
