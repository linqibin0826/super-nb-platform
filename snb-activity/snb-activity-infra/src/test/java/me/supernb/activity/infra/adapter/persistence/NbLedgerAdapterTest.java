package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// NB 账本:totalPoints 聚合口径 + 解锁/打卡两条来源的同事务记账幂等(T3/T4 追加用例)。
@SpringBootTest(classes = NbLedgerInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class NbLedgerAdapterTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/activity");
        r.add("spring.flyway.schemas", () -> "activity");
    }

    @Autowired
    NbLedgerAdapter adapter;

    @Autowired
    AchievementUnlockAdapter unlockAdapter;

    @Autowired
    CheckinAdapter checkinAdapter;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void totalPointsSumsAllRowsAndDefaultsZero() {
        assertThat(adapter.totalPoints(9101)).isZero();
        jdbc.update("INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at) "
                + "VALUES (92001, 9101, 'EARN', 'checkin_daily', '2026-07-14', 3, now())");
        jdbc.update("INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at) "
                + "VALUES (92002, 9101, 'EARN', 'achievement_unlock', 'checkin_first', 5, now())");
        assertThat(adapter.totalPoints(9101)).isEqualTo(8);
    }

    @Test
    void unlockWritesLedgerRowAtomicallyAndIdempotently() {
        boolean first = unlockAdapter.unlock(9102, "checkin_first", Instant.now(), 5, "test");
        boolean replay = unlockAdapter.unlock(9102, "checkin_first", Instant.now(), 5, "test");
        assertThat(first).isTrue();
        assertThat(replay).isFalse();
        Integer rows = jdbc.queryForObject(
                "SELECT count(*) FROM activity.nb_ledger WHERE user_id=9102 AND source_type='achievement_unlock' "
                        + "AND source_ref='checkin_first'", Integer.class);
        assertThat(rows).isEqualTo(1);
        assertThat(adapter.totalPoints(9102)).isEqualTo(5);
    }

    @Test
    void checkInWritesLedgerRowOnceOnly() {
        LocalDate day = LocalDate.of(2026, 7, 16);
        var first = checkinAdapter.checkIn(9103, day, Instant.now(), 3);
        var replay = checkinAdapter.checkIn(9103, day, Instant.now(), 3);
        assertThat(first.firstCheckinToday()).isTrue();
        assertThat(replay.firstCheckinToday()).isFalse();
        Integer rows = jdbc.queryForObject(
                "SELECT count(*) FROM activity.nb_ledger WHERE user_id=9103 AND source_type='checkin_daily' "
                        + "AND source_ref='2026-07-16'", Integer.class);
        assertThat(rows).isEqualTo(1);
        assertThat(adapter.totalPoints(9103)).isEqualTo(3);
    }
}
