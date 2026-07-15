package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
}
