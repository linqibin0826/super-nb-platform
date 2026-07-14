package me.supernb.activity.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 签到批量信号:当日候选、零点整窗口([00:00:00,00:01:00))、诈尸打卡(≥30天间隔)、
/// 区间内至少签到一次。
@Testcontainers
class CheckinMetricSignalAdapterTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static CheckinMetricSignalAdapter adapter;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE SCHEMA activity");
        jdbc.execute("CREATE TABLE activity.checkin_record (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "checkin_date DATE NOT NULL, checked_in_at TIMESTAMPTZ NOT NULL)");
        adapter = new CheckinMetricSignalAdapter(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.checkin_record");
    }

    @Test
    void usersCheckedInOnReturnsDistinctUsersForThatDay() {
        jdbc.update("INSERT INTO activity.checkin_record VALUES (1, 42, '2026-07-13', '2026-07-13T03:00:00Z')");
        jdbc.update("INSERT INTO activity.checkin_record VALUES (2, 7, '2026-07-12', '2026-07-12T03:00:00Z')");
        assertThat(adapter.usersCheckedInOn(LocalDate.of(2026, 7, 13))).containsExactly(42L);
    }

    @Test
    void usersCheckedInAtMidnightOnMatchesFirstMinuteOfShanghaiDay() {
        // 2026-07-13 00:00:30 上海时间 = 2026-07-12T16:00:30Z
        jdbc.update("INSERT INTO activity.checkin_record VALUES (1, 42, '2026-07-13', '2026-07-12T16:00:30Z')");
        jdbc.update("INSERT INTO activity.checkin_record VALUES (2, 7, '2026-07-13', '2026-07-13T03:00:00Z')");
        assertThat(adapter.usersCheckedInAtMidnightOn(LocalDate.of(2026, 7, 13))).containsExactly(42L);
    }

    @Test
    void hasGhostReturnAsOfDetectsThirtyDayGap() {
        jdbc.update("INSERT INTO activity.checkin_record VALUES (1, 9, '2026-06-01', '2026-06-01T03:00:00Z')");
        jdbc.update("INSERT INTO activity.checkin_record VALUES (2, 9, '2026-07-05', '2026-07-05T03:00:00Z')");
        assertThat(adapter.hasGhostReturnAsOf(9, LocalDate.of(2026, 7, 5))).isTrue();
        jdbc.update("INSERT INTO activity.checkin_record VALUES (3, 5, '2026-07-01', '2026-07-01T03:00:00Z')");
        jdbc.update("INSERT INTO activity.checkin_record VALUES (4, 5, '2026-07-10', '2026-07-10T03:00:00Z')");
        assertThat(adapter.hasGhostReturnAsOf(5, LocalDate.of(2026, 7, 10))).isFalse();
    }

    @Test
    void usersCheckedInBetweenReturnsAnyoneWithAtLeastOneDay() {
        jdbc.update("INSERT INTO activity.checkin_record VALUES (1, 3, '2026-07-15', '2026-07-15T03:00:00Z')");
        assertThat(adapter.usersCheckedInBetween(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 31)))
                .containsExactly(3L);
    }
}
