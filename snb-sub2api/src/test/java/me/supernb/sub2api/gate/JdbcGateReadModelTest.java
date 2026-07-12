package me.supernb.sub2api.gate;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 金票门槛读模型:只认 COMPLETED 的 balance 单,赠送/未完成/其他类型一律不计;无记录=0。
@Testcontainers
class JdbcGateReadModelTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static GateReadModel readModel;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "amount NUMERIC(20,2), order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");
        // 10=两单 COMPLETED balance 合计 30;20=一单 10;30=只有 PENDING;40=只有非 balance 类型
        order(jdbc, 10, "12.50", "COMPLETED", "balance");
        order(jdbc, 10, "17.50", "COMPLETED", "balance");
        order(jdbc, 20, "10.00", "COMPLETED", "balance");
        order(jdbc, 30, "99.00", "PENDING", "balance");
        order(jdbc, 40, "99.00", "COMPLETED", "subscription");
        readModel = new JdbcGateReadModel(jdbc);
    }

    static void order(JdbcTemplate j, long uid, String amount, String status, String type) {
        j.update("INSERT INTO payment_orders (user_id, amount, order_type, status) VALUES (?,?,?,?)",
                uid, new BigDecimal(amount), type, status);
    }

    @Test
    void sumsOnlyCompletedBalanceOrders() {
        assertThat(readModel.totalRecharged(10)).isEqualByComparingTo("30.00");
        assertThat(readModel.totalRecharged(20)).isEqualByComparingTo("10.00");
    }

    @Test
    void pendingAndNonBalanceOrdersDoNotCount() {
        assertThat(readModel.totalRecharged(30)).isEqualByComparingTo("0");
        assertThat(readModel.totalRecharged(40)).isEqualByComparingTo("0");
    }

    @Test
    void unknownUserIsZero() {
        assertThat(readModel.totalRecharged(999)).isEqualByComparingTo("0");
    }
}
