package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;
import org.junit.jupiter.api.BeforeEach;
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

/// 签到写路径:幂等 INSERT ON CONFLICT DO NOTHING、并发只有一个赢家、月度/累计读查询。
@SpringBootTest(classes = CheckinInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CheckinAdapterTest {

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

    static final LocalDate DAY = LocalDate.of(2026, 7, 13);

    @Autowired
    CheckinAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.checkin_record");
    }

    @Test
    void firstCheckInTodayInsertsAndReturnsTrue() {
        CheckinOutcome o = adapter.checkIn(42, DAY, Instant.parse("2026-07-13T01:00:00Z"));
        assertThat(o.firstCheckinToday()).isTrue();
        assertThat(o.checkinDate()).isEqualTo(DAY);
        assertThat(adapter.checkedInOn(42, DAY)).isTrue();
    }

    @Test
    void secondCheckInSameDayIsIdempotentAndReplaysOriginalTimestamp() {
        Instant first = Instant.parse("2026-07-13T01:00:00Z");
        adapter.checkIn(42, DAY, first);
        CheckinOutcome again = adapter.checkIn(42, DAY, Instant.parse("2026-07-13T09:00:00Z"));
        assertThat(again.firstCheckinToday()).isFalse();
        assertThat(again.checkedInAt()).isEqualTo(first);
        assertThat(adapter.totalCheckins(42)).isEqualTo(1);
    }

    @Test
    void concurrentCheckInsForSameUserDayOnlyOneWins() throws Exception {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<CheckinOutcome>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                gate.await();
                return adapter.checkIn(99, DAY, Instant.now());
            }));
        }
        gate.countDown();
        long trueCount = 0;
        for (Future<CheckinOutcome> f : futures) {
            if (f.get().firstCheckinToday()) {
                trueCount++;
            }
        }
        pool.shutdown();
        assertThat(trueCount).isEqualTo(1);
        assertThat(adapter.totalCheckins(99)).isEqualTo(1);
    }

    @Test
    void countAndDatesInRangeReflectStoredRows() {
        adapter.checkIn(7, LocalDate.of(2026, 7, 1), Instant.parse("2026-07-01T00:30:00Z"));
        adapter.checkIn(7, LocalDate.of(2026, 7, 3), Instant.parse("2026-07-03T00:30:00Z"));
        adapter.checkIn(7, LocalDate.of(2026, 8, 1), Instant.parse("2026-08-01T00:30:00Z"));
        assertThat(adapter.countInRange(7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31))).isEqualTo(2);
        assertThat(adapter.datesInRange(7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)))
                .containsExactly(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 1));
        assertThat(adapter.totalCheckins(7)).isEqualTo(3);
    }

    @Test
    void fullAttendanceUserIdsReturnsOnlyUsersMatchingExactCount() {
        LocalDate d1 = LocalDate.of(2026, 6, 1);
        LocalDate d2 = LocalDate.of(2026, 6, 2);
        LocalDate d3 = LocalDate.of(2026, 6, 3);
        adapter.checkIn(1, d1, d1.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        adapter.checkIn(1, d2, d2.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        adapter.checkIn(1, d3, d3.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        adapter.checkIn(2, d1, d1.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()); // 只签 1 天,不满勤
        assertThat(adapter.fullAttendanceUserIds(d1, d3, 3)).containsExactly(1L);
    }
}
