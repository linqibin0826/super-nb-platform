package me.supernb.sub2api.usageboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class JdbcUsageBoardReadModelTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static final Instant START = Instant.parse("2026-07-06T16:00:00Z"); // 周一 00:00 Asia/Shanghai
    static final Instant END = Instant.parse("2026-07-13T16:00:00Z");

    static UsageBoardReadModel readModel;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, username TEXT, "
                + "role TEXT, status TEXT, deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE user_avatars (user_id BIGINT UNIQUE, url TEXT NOT NULL DEFAULT '')");
        jdbc.execute("CREATE TABLE usage_logs (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "input_tokens INT, output_tokens INT, cache_creation_tokens INT, cache_read_tokens INT, "
                + "actual_cost DOUBLE PRECISION, created_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "order_type TEXT, status TEXT)");

        // 10=有用户名的付费用户;20=无用户名付费用户(脱敏);30=admin;40=disabled;50=软删;60=从未充值(门槛外)
        user(jdbc, 10, "alice@qq.com", "老王", "user", "active", null);
        user(jdbc, 20, "1234567@qq.com", null, "user", "active", null);
        user(jdbc, 30, "admin@x.com", null, "admin", "active", null);
        user(jdbc, 40, "dis@x.com", null, "user", "disabled", null);
        user(jdbc, 50, "del@x.com", null, "user", "active", "2026-07-01T00:00:00Z");
        user(jdbc, 60, "free@x.com", null, "user", "active", null);
        jdbc.update("INSERT INTO user_avatars(user_id, url) VALUES (10, 'https://cdn/a.png'), (20, '')");
        // 门槛:10/20/30/40/50 有 COMPLETED balance 单;60 只有未完成单
        for (long uid : new long[] {10, 20, 30, 40, 50}) {
            jdbc.update("INSERT INTO payment_orders(user_id, order_type, status) VALUES (?, 'balance', 'COMPLETED')", uid);
        }
        jdbc.update("INSERT INTO payment_orders(user_id, order_type, status) VALUES (60, 'balance', 'PENDING')");
        // 用量:uid10 两条(窗口内);uid20 一条;uid30/40/50/60 各一条;另有 uid10 窗口外一条(边界排他)
        log(jdbc, 10, 100, 50, 10, 40, 1.5, "2026-07-07T01:00:00Z");
        log(jdbc, 10, 200, 100, 0, 0, 2.5, "2026-07-08T01:00:00Z");
        log(jdbc, 20, 1000, 0, 0, 0, 9.0, "2026-07-07T02:00:00Z");
        log(jdbc, 30, 1, 1, 1, 1, 0.1, "2026-07-07T02:00:00Z");
        log(jdbc, 40, 1, 1, 1, 1, 0.1, "2026-07-07T02:00:00Z");
        log(jdbc, 50, 1, 1, 1, 1, 0.1, "2026-07-07T02:00:00Z");
        log(jdbc, 60, 1, 1, 1, 1, 0.1, "2026-07-07T02:00:00Z");
        log(jdbc, 10, 999, 0, 0, 0, 9.9, "2026-07-13T16:00:00Z"); // == END,排他不计
        readModel = new JdbcUsageBoardReadModel(jdbc);
    }

    static void user(JdbcTemplate j, long id, String email, String username, String role, String status, String deletedAt) {
        j.update("INSERT INTO users(id, email, username, role, status, deleted_at) VALUES (?,?,?,?,?,?)",
                id, email, username, role, status, deletedAt == null ? null : Timestamp.from(Instant.parse(deletedAt)));
    }

    static void log(JdbcTemplate j, long uid, int in, int out, int cc, int cr, double cost, String at) {
        j.update("INSERT INTO usage_logs(user_id, input_tokens, output_tokens, cache_creation_tokens, "
                + "cache_read_tokens, actual_cost, created_at) VALUES (?,?,?,?,?,?,?)",
                uid, in, out, cc, cr, cost, Timestamp.from(Instant.parse(at)));
    }

    @Test
    void aggregatesTokensRequestsAndCostPerEligibleUser() {
        List<UsageBoardReadModel.UsageRow> rows = readModel.aggregate(START, END);
        assertThat(rows).hasSize(2); // 只剩 10 和 20
        UsageBoardReadModel.UsageRow u10 = rows.stream().filter(r -> r.userId() == 10).findFirst().orElseThrow();
        assertThat(u10.tokens()).isEqualTo(100 + 50 + 10 + 40 + 200 + 100); // 500,窗口外那条不计
        assertThat(u10.requests()).isEqualTo(2);
        assertThat(u10.cost()).isEqualTo(4.0);
        assertThat(u10.displayName()).isEqualTo("老王");        // username 优先
        assertThat(u10.avatarUrl()).isEqualTo("https://cdn/a.png");
    }

    @Test
    void masksEmailWhenUsernameAbsentAndNullsEmptyAvatar() {
        UsageBoardReadModel.UsageRow u20 = readModel.aggregate(START, END).stream()
                .filter(r -> r.userId() == 20).findFirst().orElseThrow();
        assertThat(u20.displayName()).isEqualTo("12***67@qq.com"); // 前2+***+后2(本地段≥5)
        assertThat(u20.avatarUrl()).isNull();                      // 空串 → null
    }

    @Test
    void excludesAdminDisabledDeletedAndNonPayers() {
        assertThat(readModel.aggregate(START, END))
                .extracting(UsageBoardReadModel.UsageRow::userId)
                .containsExactlyInAnyOrder(10L, 20L); // 30 admin/40 disabled/50 软删/60 未充值全排除
    }

    @Test
    void eligibleReflectsCompletedBalanceOrders() {
        assertThat(readModel.eligible(10)).isTrue();
        assertThat(readModel.eligible(60)).isFalse(); // 只有 PENDING 单
        assertThat(readModel.eligible(999)).isFalse();
    }

    @Test
    void emptyWindowYieldsEmptyList() {
        assertThat(readModel.aggregate(Instant.parse("2020-01-01T00:00:00Z"),
                Instant.parse("2020-01-02T00:00:00Z"))).isEmpty();
    }
}
