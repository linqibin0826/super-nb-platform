package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.NoDrawsLeftException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
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

/// 并发抽奖不超额:advisory lock 串行化同一用户,发放数永远 = min(应得次数, 池容量),多余请求一律 NoDrawsLeft。
/// 真实 Spring 上下文(JPA 适配器)+ Testcontainers PG + Flyway 建 activity schema。
@SpringBootTest(classes = ActivityInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class DrawAdapterConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

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

    /// 造数:id 显式给值(雪花基座后无数据库自增,纯 SQL 写入必须带 id)。
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

    Map<String, Long> raceDraw(Campaign campaign, int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        Map<String, LongAdder> tally = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                startGate.await();
                String outcome;
                try {
                    DrawResult r = adapter.drawFor(campaign, USER);
                    outcome = r.consolation() ? "CONSOLATION" : "PRIZE";
                } catch (NoDrawsLeftException e) {
                    outcome = "NO_DRAWS";
                }
                tally.computeIfAbsent(outcome, k -> new LongAdder()).increment();
                return null;
            }));
        }
        startGate.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        Map<String, Long> result = new ConcurrentHashMap<>();
        tally.forEach((k, v) -> result.put(k, v.sum()));
        return result;
    }

    @Test
    void concurrentDrawsNeverExceedEarnedWhenPoolAmple() throws Exception {
        Campaign campaign = seed(10); // 10 槽,应得 3
        Map<String, Long> tally = raceDraw(campaign, 10);

        assertThat(tally.getOrDefault("PRIZE", 0L)).isEqualTo(3);
        assertThat(tally.getOrDefault("NO_DRAWS", 0L)).isEqualTo(7);
        assertThat(tally.getOrDefault("CONSOLATION", 0L)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.draw WHERE user_id = ?", Integer.class, USER))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.prize_slot WHERE status = 'claimed'", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void poolSmallerThanEarnedFallsBackToConsolation() throws Exception {
        Campaign campaign = seed(2); // 只 2 槽,应得 3 → 2 奖 + 1 安慰
        Map<String, Long> tally = raceDraw(campaign, 10);

        assertThat(tally.getOrDefault("PRIZE", 0L)).isEqualTo(2);
        assertThat(tally.getOrDefault("CONSOLATION", 0L)).isEqualTo(1);
        assertThat(tally.getOrDefault("NO_DRAWS", 0L)).isEqualTo(7);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.draw WHERE user_id = ?", Integer.class, USER))
                .isEqualTo(3);
    }

    @Test
    void drawRowsCarrySnowflakeIdAndAuditColumns() throws Exception {
        Campaign campaign = seed(10);
        raceDraw(campaign, 1);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT id, created_at, updated_at, version, created_by FROM activity.draw WHERE user_id = ?", USER);
        assertThat(((Number) row.get("id")).longValue()).isGreaterThan(1_000_000_000L); // 雪花量级,非小自增
        assertThat(row.get("created_at")).isNotNull();
        assertThat(row.get("updated_at")).isNotNull();
        assertThat(((Number) row.get("version")).longValue()).isZero();
        assertThat(row.get("created_by")).isNull(); // 无请求上下文 → auditor empty → 留 NULL
    }
}
