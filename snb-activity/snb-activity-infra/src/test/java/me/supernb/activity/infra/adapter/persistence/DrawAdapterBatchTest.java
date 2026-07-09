package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.NoDrawsLeftException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import org.junit.jupiter.api.AfterEach;
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

/// 批量抽奖:一个事务原子抽 min(剩余,10),每领一槽显式 flush 防 native SKIP LOCKED 重选。
@SpringBootTest(classes = ActivityInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class DrawAdapterBatchTest {

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

    static final long USER = 42L;
    static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    @Autowired
    DrawAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void resetRecharge() {
        ActivityInfraTestApp.recharge = new BigDecimal("300");
    }

    Campaign seed(int slots) {
        jdbc.execute("TRUNCATE activity.draw, activity.prize_slot, activity.campaign");
        Long cid = jdbc.queryForObject(
                "INSERT INTO activity.campaign (id, name, starts_at, ends_at, status, consolation_amount) "
                        + "VALUES (1, 'c', ?, ?, 'active', 5) RETURNING id",
                Long.class, java.sql.Timestamp.from(START), java.sql.Timestamp.from(END));
        for (int i = 0; i < slots; i++) {
            jdbc.update("INSERT INTO activity.prize_slot (id, campaign_id, amount, redeem_code, status) "
                    + "VALUES (?, ?, 10, ?, 'available')", 1000 + i, cid, "CODE" + i);
        }
        return new Campaign(cid, "c", START, END, "active", new BigDecimal("5"));
    }

    @Test
    void drawAllReturnsDistinctPrizesUpToEarned() {
        Campaign campaign = seed(10); // earned 3(充值默认 300)
        List<DrawResult> results = adapter.drawAllFor(campaign, USER);

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(r -> !r.consolation());
        assertThat(results.stream().map(DrawResult::redeemCode).distinct().count()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.prize_slot WHERE status='claimed'", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.draw WHERE user_id=?", Integer.class, USER)).isEqualTo(3);
    }

    @Test
    void drawAllCapsAtBatchMax() {
        ActivityInfraTestApp.recharge = new BigDecimal("1500"); // earned 15
        Campaign campaign = seed(20);
        List<DrawResult> results = adapter.drawAllFor(campaign, USER);

        assertThat(results).hasSize(10);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.prize_slot WHERE status='claimed'", Integer.class)).isEqualTo(10);
        assertThat(adapter.countDraws(campaign.id(), USER)).isEqualTo(10);
    }

    @Test
    void drawAllFallsBackToConsolationWhenPoolDrains() {
        ActivityInfraTestApp.recharge = new BigDecimal("500"); // earned 5
        Campaign campaign = seed(2);
        List<DrawResult> results = adapter.drawAllFor(campaign, USER);

        assertThat(results).hasSize(5);
        assertThat(results.stream().filter(r -> !r.consolation()).count()).isEqualTo(2);
        assertThat(results.stream().filter(DrawResult::consolation).count()).isEqualTo(3);
    }

    @Test
    void drawAllNoDrawsLeftThrows() {
        ActivityInfraTestApp.recharge = BigDecimal.ZERO; // earned 0
        Campaign campaign = seed(10);
        assertThatThrownBy(() -> adapter.drawAllFor(campaign, USER))
                .isInstanceOf(NoDrawsLeftException.class);
    }
}
