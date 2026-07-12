package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.model.gate.GateDrawOutcome;
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

/// 金票闸机事务体:每日唯一仲裁、SKIP LOCKED 单张领码、防丢码回放、池空即未中。
@SpringBootTest(classes = GateInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class GateAdapterTest {

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

    static final LocalDate DAY = LocalDate.of(2026, 7, 12);

    @Autowired
    GateAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.gate_attempt, activity.gate_ticket");
    }

    void seedTickets(int n) {
        for (int i = 1; i <= n; i++) {
            jdbc.update("INSERT INTO activity.gate_ticket (id, amount, code) VALUES (?, ?, ?)",
                    900000L + i, java.math.BigDecimal.valueOf(3), "FAKE-" + i);
        }
    }

    long availableCount() {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM activity.gate_ticket WHERE claimed_by IS NULL", Long.class);
        return n == null ? -1 : n;
    }

    @Test
    void firstWinClaimsOneTicketAtomically() {
        seedTickets(3);
        GateDrawOutcome o = adapter.drawFor(7L, DAY, true);
        assertThat(o.win()).isTrue();
        assertThat(o.code()).isNotBlank();
        assertThat(o.amount()).isNotNull();
        assertThat(o.drawnAt()).isNotNull();
        assertThat(availableCount()).isEqualTo(2);
    }

    @Test
    void sameDaySecondCallReturnsSameTicketWithoutNewClaim() {
        seedTickets(2);
        GateDrawOutcome first = adapter.drawFor(7L, DAY, true);
        GateDrawOutcome again = adapter.drawFor(7L, DAY, true);
        assertThat(again.win()).isTrue();
        assertThat(again.code()).isEqualTo(first.code());
        assertThat(availableCount()).isEqualTo(1);
    }

    @Test
    void loseRecordsAttemptAndBlocksRetry() {
        seedTickets(1);
        GateDrawOutcome o = adapter.drawFor(8L, DAY, false);
        assertThat(o.win()).isFalse();
        GateDrawOutcome retry = adapter.drawFor(8L, DAY, true);
        assertThat(retry.win()).isFalse();
        assertThat(availableCount()).isEqualTo(1);
    }

    @Test
    void emptyPoolMeansLoseButStillRecordsAttempt() {
        GateDrawOutcome o = adapter.drawFor(9L, DAY, true);
        assertThat(o.win()).isFalse();
        Long attempts = jdbc.queryForObject(
                "SELECT COUNT(*) FROM activity.gate_attempt WHERE user_id = 9", Long.class);
        assertThat(attempts).isEqualTo(1);
    }
}
