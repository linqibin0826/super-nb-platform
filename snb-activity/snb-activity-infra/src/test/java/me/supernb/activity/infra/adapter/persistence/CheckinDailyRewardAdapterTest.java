package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardRecord;
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

/// 每日返网费台账:占位幂等、失败累加尝试次数、可重试集合筛选、按月汇总。
@SpringBootTest(classes = CheckinDailyRewardInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CheckinDailyRewardAdapterTest {

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
    CheckinDailyRewardAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.checkin_daily_reward");
    }

    @Test
    void claimIsIdempotentPerUserPerDay() {
        LocalDate day = LocalDate.of(2026, 8, 5);
        Optional<Long> first =
                adapter.claim(42L, day, 5, 15, new BigDecimal("0.50"), "pending", "checkin-daily-2026-08-05");
        Optional<Long> second =
                adapter.claim(42L, day, 5, 15, new BigDecimal("0.50"), "pending", "checkin-daily-2026-08-05");
        assertThat(first).isPresent();
        assertThat(second).isEmpty();
    }

    @Test
    void markFailedAccumulatesAttempts() {
        LocalDate day = LocalDate.of(2026, 8, 6);
        long id = adapter.claim(43L, day, 1, 3, new BigDecimal("0.10"), "pending", "checkin-daily-2026-08-06")
                .orElseThrow();
        adapter.markFailed(id, "上游 500");
        adapter.markFailed(id, "上游 500");
        CheckinDailyRewardRecord r = adapter.findByUserAndDay(43L, day).orElseThrow();
        assertThat(r.attempts()).isEqualTo(2);
        assertThat(r.balanceStatus()).isEqualTo("failed");
    }

    @Test
    void retryableExcludesExhaustedAndSuccessRows() {
        LocalDate day = LocalDate.of(2026, 8, 7);
        long ok = adapter.claim(44L, day, 1, 3, new BigDecimal("0.10"), "pending", "n").orElseThrow();
        adapter.markSuccess(ok);
        long dead = adapter.claim(45L, day, 1, 3, new BigDecimal("0.10"), "pending", "n").orElseThrow();
        adapter.markFailed(dead, "e");
        adapter.markFailed(dead, "e");
        adapter.markFailed(dead, "e");
        long alive = adapter.claim(46L, day, 1, 3, new BigDecimal("0.10"), "pending", "n").orElseThrow();
        assertThat(adapter.retryable(3)).extracting(CheckinDailyRewardRecord::id).containsExactly(alive);
    }

    @Test
    void monthlyBalanceTotalSumsOnlyWindow() {
        adapter.claim(47L, LocalDate.of(2026, 8, 1), 1, 3, new BigDecimal("0.10"), "success", "n");
        adapter.claim(47L, LocalDate.of(2026, 8, 2), 2, 6, new BigDecimal("0.20"), "success", "n");
        adapter.claim(47L, LocalDate.of(2026, 9, 1), 1, 3, new BigDecimal("0.10"), "success", "n");
        assertThat(adapter.monthlyBalanceTotal(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .isEqualByComparingTo("0.30");
        assertThat(adapter.myMonthlyBalanceTotal(47L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .isEqualByComparingTo("0.30");
    }
}
