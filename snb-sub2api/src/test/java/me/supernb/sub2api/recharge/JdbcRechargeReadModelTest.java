package me.supernb.sub2api.recharge;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcRechargeReadModelTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    static RechargeReadModel readModel;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, role TEXT)");
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "amount NUMERIC(20,2), order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE redeem_codes (code TEXT PRIMARY KEY, status TEXT, expires_at TIMESTAMPTZ)");

        jdbc.update("INSERT INTO users VALUES (1,'alice@qq.com','user'),(2,'bob@gmail.com','user'),(3,'admin@x.com','admin')");

        // user1 窗口内 balance/COMPLETED:100 + 60 + 5(<10)= 165
        insertOrder(jdbc, 1, "100", "balance", "COMPLETED", "2026-07-10T00:00:00Z");
        insertOrder(jdbc, 1, "60", "balance", "COMPLETED", "2026-07-12T00:00:00Z");
        insertOrder(jdbc, 1, "5", "balance", "COMPLETED", "2026-07-13T00:00:00Z");
        // user2 窗口内 250
        insertOrder(jdbc, 2, "250", "balance", "COMPLETED", "2026-07-11T00:00:00Z");
        // admin 500(role 排除)
        insertOrder(jdbc, 3, "500", "balance", "COMPLETED", "2026-07-11T00:00:00Z");
        // 各类应排除项
        insertOrder(jdbc, 1, "999", "balance", "COMPLETED", "2026-06-15T00:00:00Z"); // 窗口外
        insertOrder(jdbc, 2, "40", "balance", "PENDING", "2026-07-11T00:00:00Z");     // 未完成
        insertOrder(jdbc, 1, "30", "recharge", "COMPLETED", "2026-07-11T00:00:00Z");  // 非 balance

        jdbc.update("INSERT INTO redeem_codes VALUES ('C1','unused',?)", Timestamp.from(Instant.parse("2026-07-30T00:00:00Z")));

        readModel = new JdbcRechargeReadModel(jdbc);
    }

    static void insertOrder(JdbcTemplate jdbc, long uid, String amount, String type, String status, String at) {
        jdbc.update("INSERT INTO payment_orders (user_id, amount, order_type, status, completed_at) VALUES (?,?,?,?,?)",
                uid, new BigDecimal(amount), type, status, Timestamp.from(Instant.parse(at)));
    }

    @Test
    void totalRechargeCountsOnlyBalanceCompletedInWindow() {
        assertThat(readModel.totalRecharge(1, START, END)).isEqualByComparingTo("165");
        assertThat(readModel.totalRecharge(2, START, END)).isEqualByComparingTo("250");
    }

    @Test
    void leaderboardExcludesAdminAndMasksEmails() {
        List<RechargeReadModel.LeaderRow> board = readModel.leaderboard(START, END, 10);
        assertThat(board).hasSize(2);
        assertThat(board.get(0).name()).isEqualTo("bo***@gmail.com");
        assertThat(board.get(0).amount()).isEqualByComparingTo("250");
        assertThat(board.get(1).name()).isEqualTo("al***@qq.com");
        assertThat(board.get(1).amount()).isEqualByComparingTo("165");
    }

    @Test
    void recentRechargesFiltersSmallAndOrdersByTimeDesc() {
        List<RechargeReadModel.RechargeRow> recent = readModel.recentRecharges(START, END, 10);
        // 排除 <¥10 的 5、admin、窗口外/未完成/非 balance;剩 100/60/250,按时间倒序
        assertThat(recent).hasSize(3);
        assertThat(recent.get(0).amount()).isEqualByComparingTo("60");   // 07-12 最新
        assertThat(recent.get(1).amount()).isEqualByComparingTo("250");  // 07-11
        assertThat(recent.get(2).amount()).isEqualByComparingTo("100");  // 07-10
        assertThat(recent).allSatisfy(r -> assertThat(r.name()).contains("***"));
    }

    @Test
    void maskedEmailsExcludesAdmin() {
        Map<Long, String> emails = readModel.maskedEmailsByIds(List.of(1L, 2L, 3L));
        assertThat(emails).containsOnlyKeys(1L, 2L);
        assertThat(emails.get(1L)).isEqualTo("al***@qq.com");
    }

    @Test
    void codeStatusesReturnsFoundOnly() {
        Map<String, RechargeReadModel.RedeemCodeStatus> statuses = readModel.codeStatuses(List.of("C1", "CX"));
        assertThat(statuses).containsOnlyKeys("C1");
        assertThat(statuses.get("C1").status()).isEqualTo("unused");
    }
}
