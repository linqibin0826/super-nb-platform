package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import me.supernb.activity.domain.model.raffle.RaffleEntrant;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;
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

/// 报名适配器:幂等回执、参会证并发取号不撞不跳、读查询映射。
@SpringBootTest(classes = RaffleInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class RaffleEntryAdapterTest {

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

    @Autowired RaffleEntryAdapter adapter;
    @Autowired RaffleCampaignAdapter campaigns;
    @Autowired RafflePrizeAdapter prizes;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE activity.raffle_entry, activity.raffle_prize, activity.raffle_campaign");
        jdbc.update("INSERT INTO activity.raffle_campaign (id, name, entry_open_at, entry_close_at, draw_at, "
                + "gate_type, gate_amount, gate_from) VALUES (1, '第一届发布会', now() - interval '1 day', "
                + "now() + interval '1 day', now() + interval '1 day', 'RECHARGE', 100, '2026-01-01')");
        jdbc.update("INSERT INTO activity.raffle_prize (id, campaign_id, tier, display_name, kind, payload, sort_order) "
                + "VALUES (1001, 1, 'S', '疯狂星期四专项(V我50)', 'ALIPAY_CODE', 'FAKE-KFC-50', 0)");
    }

    @Test
    void enterIsIdempotentPerUser() {
        RaffleEntryTicket first = adapter.enter(1, 42, new BigDecimal("130"), "1.2.3.4", "UA");
        RaffleEntryTicket again = adapter.enter(1, 42, new BigDecimal("150"), "1.2.3.4", "UA");
        assertThat(first.already()).isFalse();
        assertThat(again.already()).isTrue();
        assertThat(again.entryNo()).isEqualTo(first.entryNo());
        assertThat(adapter.count(1)).isEqualTo(1);
    }

    @Test
    void entryNoIsSequentialAndConcurrencySafe() throws Exception {
        int users = 20;
        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch gate = new CountDownLatch(1);
        List<Future<RaffleEntryTicket>> futures = new ArrayList<>();
        for (int i = 0; i < users; i++) {
            long uid = 100 + i;
            futures.add(pool.submit(() -> {
                gate.await();
                return adapter.enter(1, uid, BigDecimal.TEN, null, null);
            }));
        }
        gate.countDown();
        List<Integer> nos = new ArrayList<>();
        for (Future<RaffleEntryTicket> f : futures) {
            nos.add(f.get().entryNo());
        }
        pool.shutdown();
        assertThat(nos).doesNotHaveDuplicates();
        assertThat(nos).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(1, users).boxed().toList()); // 连续 1..20 不跳号
    }

    @Test
    void findRecentEntrantsAndCampaignQueriesMap() {
        adapter.enter(1, 42, new BigDecimal("130"), null, null);
        assertThat(adapter.find(1, 42)).map(RaffleEntrant::entryNo).contains(1);
        assertThat(adapter.recent(1, 12)).extracting(RaffleEntrant::userId).containsExactly(42L);
        assertThat(adapter.entrants(1)).hasSize(1);
        assertThat(campaigns.current()).isPresent();
        assertThat(campaigns.byId(1)).map(c -> c.gateAmount().intValue()).contains(100);
        assertThat(campaigns.dueForDraw(Instant.now().plus(Duration.ofDays(2)))).hasSize(1);
        assertThat(prizes.byCampaign(1)).hasSize(1);
        assertThat(prizes.wonBy(1, 42)).isEmpty();
    }
}
