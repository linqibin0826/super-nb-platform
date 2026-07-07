package me.supernb.activity.infra;

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
import me.supernb.activity.app.ActivityDto;
import me.supernb.activity.app.RechargeQueryPort;
import me.supernb.activity.domain.Campaign;
import me.supernb.activity.domain.DrawResult;
import me.supernb.activity.domain.NoDrawsLeftException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 并发抽奖不超额:advisory lock 串行化同一用户,发放数永远 = min(应得次数, 池容量),多余请求一律 NoDrawsLeft。
@Testcontainers
class DrawAdapterConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    static final long USER = 42L;
    static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    static JdbcTemplate jdbc;
    static PlatformTransactionManager txm;

    /// 充值总额固定 ¥300 → 应得 3 次抽奖。
    static final RechargeQueryPort RECHARGE_300 = new RechargeQueryPort() {
        @Override
        public BigDecimal totalRecharge(long userId, Instant start, Instant end) {
            return new BigDecimal("300");
        }

        @Override
        public List<ActivityDto.LeaderEntry> leaderboard(Instant s, Instant e, int limit) {
            return List.of();
        }

        @Override
        public List<ActivityDto.RechargeEntry> recentRecharges(Instant s, Instant e, int limit) {
            return List.of();
        }

        @Override
        public Map<Long, String> maskedEmailsByIds(java.util.Collection<Long> ids) {
            return Map.of();
        }

        @Override
        public Map<String, ActivityDto.CodeStatus> codeStatuses(java.util.Collection<String> codes) {
            return Map.of();
        }
    };

    @BeforeAll
    static void init() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        jdbc = new JdbcTemplate(ds);
        txm = new DataSourceTransactionManager(ds);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS activity");
        jdbc.execute("CREATE TABLE activity.campaign (id BIGSERIAL PRIMARY KEY, name TEXT, "
                + "starts_at TIMESTAMPTZ, ends_at TIMESTAMPTZ, status TEXT, consolation_amount NUMERIC(20,2))");
        jdbc.execute("CREATE TABLE activity.prize_slot (id BIGSERIAL PRIMARY KEY, campaign_id BIGINT, "
                + "amount NUMERIC(20,2), redeem_code TEXT, status TEXT DEFAULT 'available', "
                + "claimed_by BIGINT, claimed_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE activity.draw (id BIGSERIAL PRIMARY KEY, campaign_id BIGINT, user_id BIGINT, "
                + "slot_id BIGINT, amount NUMERIC(20,2), redeem_code TEXT, is_consolation BOOLEAN DEFAULT false, "
                + "created_at TIMESTAMPTZ DEFAULT now())");
    }

    Campaign seed(int slots) {
        jdbc.execute("TRUNCATE activity.draw, activity.prize_slot, activity.campaign RESTART IDENTITY");
        Long cid = jdbc.queryForObject(
                "INSERT INTO activity.campaign (name, starts_at, ends_at, status, consolation_amount) "
                        + "VALUES ('c', ?, ?, 'active', 5) RETURNING id",
                Long.class, java.sql.Timestamp.from(START), java.sql.Timestamp.from(END));
        for (int i = 0; i < slots; i++) {
            jdbc.update("INSERT INTO activity.prize_slot (campaign_id, amount, redeem_code, status) "
                    + "VALUES (?, 10, ?, 'available')", cid, "CODE" + i);
        }
        return new Campaign(cid, "c", START, END, "active", new BigDecimal("5"));
    }

    Map<String, Long> raceDraw(Campaign campaign, int threads) throws Exception {
        DrawAdapter adapter = new DrawAdapter(jdbc, txm, RECHARGE_300);
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
}
