package me.supernb.sub2api.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 可开票订单口径:仅 balance+COMPLETED;orderNo 优先 out_trade_no、空则回退 id 文本;
/// balance 查无此人返回 0;emailsByIds 返回完整邮箱(仅 admin 消费)。
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class JdbcInvoiceOrderReadModelTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static InvoiceOrderReadModel readModel;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, role TEXT, balance DOUBLE PRECISION)");
        jdbc.execute("CREATE TABLE payment_orders (id BIGINT PRIMARY KEY, user_id BIGINT, amount NUMERIC(20,2), "
                + "out_trade_no TEXT, order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");
        jdbc.update("INSERT INTO users VALUES (1,'alice@qq.com','user',88.5),(2,'bob@gmail.com','user',0)");
        insert(jdbc, 11, 1, "600", "T600", "balance", "COMPLETED", "2026-07-01T00:00:00Z");
        insert(jdbc, 12, 1, "500", null, "balance", "COMPLETED", "2026-07-02T00:00:00Z");   // 无 out_trade_no
        insert(jdbc, 13, 1, "999", "TP", "balance", "PENDING", "2026-07-03T00:00:00Z");     // 未完成,排除
        insert(jdbc, 14, 1, "30", "TS", "subscription", "COMPLETED", "2026-07-04T00:00:00Z"); // 非 balance,排除
        insert(jdbc, 15, 2, "250", "T250", "balance", "COMPLETED", "2026-07-05T00:00:00Z");
        readModel = new JdbcInvoiceOrderReadModel(jdbc);
    }

    static void insert(JdbcTemplate jdbc, long id, long uid, String amount, String tradeNo,
            String type, String status, String at) {
        jdbc.update("INSERT INTO payment_orders VALUES (?,?,?,?,?,?,?)",
                id, uid, new BigDecimal(amount), tradeNo, type, status, Timestamp.from(Instant.parse(at)));
    }

    @Test
    void listsOnlyCompletedBalanceOrdersNewestFirst() {
        List<InvoiceOrderReadModel.OrderRow> rows = readModel.completedBalanceOrders(1);
        assertThat(rows).extracting(InvoiceOrderReadModel.OrderRow::id).containsExactly(12L, 11L);
        assertThat(rows.get(0).orderNo()).isEqualTo("12");     // out_trade_no 空回退 id 文本
        assertThat(rows.get(1).orderNo()).isEqualTo("T600");
        assertThat(rows.get(1).amount()).isEqualByComparingTo("600");
    }

    @Test
    void balanceOfReadsUsersTableAndDefaultsToZero() {
        assertThat(readModel.balanceOf(1)).isEqualByComparingTo("88.5");
        assertThat(readModel.balanceOf(999)).isEqualByComparingTo("0");
    }

    @Test
    void emailsByIdsReturnsFullEmailsAndSkipsUnknown() {
        Map<Long, String> emails = readModel.emailsByIds(List.of(1L, 2L, 999L));
        assertThat(emails).containsEntry(1L, "alice@qq.com").containsEntry(2L, "bob@gmail.com").hasSize(2);
        assertThat(readModel.emailsByIds(List.of())).isEmpty();
    }
}
