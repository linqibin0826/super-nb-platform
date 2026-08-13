package me.supernb.sub2api.recharge;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// firstCompletedOrder:人生第一笔 COMPLETED 付款订单(balance/subscription 均算,
/// 开学季首充礼定档用)——取最早 completed_at,忽略未完成单,无单返回 empty。
@Testcontainers
class JdbcRechargeReadModelFirstOrderTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static RechargeReadModel readModel;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "amount NUMERIC(20,2), order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");
        // 其余方法用到的表建空壳,防同类构造路径炸(本测试只打 firstCompletedOrder)
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, username TEXT, role TEXT, "
                + "created_at TIMESTAMPTZ, deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE redeem_codes (id BIGSERIAL PRIMARY KEY, code TEXT, status TEXT, "
                + "expires_at TIMESTAMPTZ)");

        // 用户 1:订阅单(08-15) 早于余额单(08-16) → 首单=订阅 ¥30
        order(1, "subscription", "COMPLETED", "30.00", "2026-08-15T01:00:00Z");
        order(1, "balance", "COMPLETED", "100.00", "2026-08-16T01:00:00Z");
        // 用户 2:只有 PENDING 单 → 视同没充过
        order(2, "balance", "PENDING", "50.00", null);

        readModel = new JdbcRechargeReadModel(jdbc);
    }

    static void order(long uid, String type, String status, String amount, String completedAt) {
        jdbc.update("INSERT INTO payment_orders (user_id, amount, order_type, status, completed_at) "
                        + "VALUES (?,?,?,?,?)",
                uid, new BigDecimal(amount), type, status,
                completedAt == null ? null : Timestamp.from(Instant.parse(completedAt)));
    }

    @Test
    void picksEarliestCompletedAcrossOrderTypes() {
        RechargeReadModel.FirstOrder first = readModel.firstCompletedOrder(1L).orElseThrow();
        assertThat(first.amountCny()).isEqualByComparingTo("30.00");
        assertThat(first.completedAt()).isEqualTo(Instant.parse("2026-08-15T01:00:00Z"));
    }

    @Test
    void ignoresPendingOrders() {
        assertThat(readModel.firstCompletedOrder(2L)).isEmpty();
    }

    @Test
    void unknownUserIsEmpty() {
        assertThat(readModel.firstCompletedOrder(999L)).isEmpty();
    }
}
