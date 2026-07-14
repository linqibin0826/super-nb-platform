package me.supernb.sub2api.recharge;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcRechargeReadModelNewRechargeTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcRechargeReadModel model;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE TABLE payment_orders (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "order_type TEXT NOT NULL, status TEXT NOT NULL, amount NUMERIC(12,2) NOT NULL, "
                + "completed_at TIMESTAMPTZ)");
        model = new JdbcRechargeReadModel(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE payment_orders");
    }

    @Test
    void usersWithNewRechargeSinceOnlyCountsCompletedBalanceOrdersInWindow() {
        jdbc.update("INSERT INTO payment_orders VALUES (1, 42, 'balance', 'COMPLETED', 30, ?)",
                Timestamp.from(Instant.parse("2026-07-13T01:00:00Z")));
        jdbc.update("INSERT INTO payment_orders VALUES (2, 7, 'balance', 'PENDING', 30, NULL)"); // 未完成,不计
        jdbc.update("INSERT INTO payment_orders VALUES (3, 9, 'balance', 'COMPLETED', 30, ?)",
                Timestamp.from(Instant.parse("2026-07-10T01:00:00Z"))); // 窗口外,不计
        var users = model.usersWithNewRechargeSince(
                Instant.parse("2026-07-12T00:00:00Z"), Instant.parse("2026-07-14T00:00:00Z"));
        assertThat(users).containsExactly(42L);
    }
}
