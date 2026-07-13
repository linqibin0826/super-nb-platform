package me.supernb.sub2api.raffle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

/// 门槛读模型两口径:RECHARGE=COMPLETED 余额单(completed_at ∈ [from,to))+已核销余额
/// 兑换码(used_at ∈ [from,to),剔除 ZPay 完成单自动建的同码镜像防双算),
/// SPEND=billing_type=0 余额扣费(created_at ∈ [from,to));批量缺席语义;展示名契约。
@Testcontainers
class JdbcRaffleGateReadModelTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant TO = Instant.parse("2026-07-13T00:00:00Z");

    static RaffleGateReadModel model;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, username TEXT, "
                + "created_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "order_type TEXT, status TEXT, amount NUMERIC(20,2), completed_at TIMESTAMPTZ, "
                + "recharge_code TEXT)");
        jdbc.execute("CREATE TABLE usage_logs (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "actual_cost DOUBLE PRECISION, billing_type SMALLINT NOT NULL DEFAULT 0, created_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE redeem_codes (id BIGSERIAL PRIMARY KEY, code TEXT UNIQUE, "
                + "type TEXT, value NUMERIC(20,8), status TEXT, used_by BIGINT, used_at TIMESTAMPTZ)");

        jdbc.update("INSERT INTO users(id, email, username, created_at) VALUES "
                + "(10, 'alice@qq.com', '老王', '2026-06-01T00:00:00Z'), "
                + "(20, '1234567@qq.com', NULL, '2026-07-10T00:00:00Z')");
        // uid10 充值:窗口内 60+40(=100);窗口下界前一笔 999(不计);窗口上界恰好 TO 一笔 999(排他不计);
        // 非 COMPLETED 一笔、非 balance 一笔(都不计);60 那笔带 ZPay 镜像码——剔除不双算(否则 160)
        pay(jdbc, 10, "60", "COMPLETED", "balance", "2026-07-02T00:00:00Z", "PAY-10-60");
        code(jdbc, "PAY-10-60", "balance", "60", "used", 10L, "2026-07-02T00:00:01Z");
        pay(jdbc, 10, "40", "COMPLETED", "balance", "2026-07-12T23:59:59Z");
        pay(jdbc, 10, "999", "COMPLETED", "balance", "2026-06-30T23:59:59Z");
        pay(jdbc, 10, "999", "COMPLETED", "balance", "2026-07-13T00:00:00Z");
        pay(jdbc, 10, "999", "PENDING", "balance", "2026-07-02T00:00:00Z");
        pay(jdbc, 10, "999", "COMPLETED", "subscription", "2026-07-02T00:00:00Z");
        // uid20 消费:窗口内余额扣费 3.5+1.5(=5.0);订阅计费 99 不计;窗口外 99 不计
        spend(jdbc, 20, 3.5, 0, "2026-07-02T00:00:00Z");
        spend(jdbc, 20, 1.5, 0, "2026-07-03T00:00:00Z");
        spend(jdbc, 20, 99.0, 1, "2026-07-04T00:00:00Z");
        spend(jdbc, 20, 99.0, 0, "2026-06-30T00:00:00Z");
        // uid30 兑换码口径:在线支付 60(镜像码剔除)+窗口内购码 25(=85);订阅码/窗口外码/未核销码不计
        pay(jdbc, 30, "60", "COMPLETED", "balance", "2026-07-03T00:00:00Z", "PAY-30-1");
        code(jdbc, "PAY-30-1", "balance", "60", "used", 30L, "2026-07-03T00:00:01Z");
        code(jdbc, "XY-30-A", "balance", "25", "used", 30L, "2026-07-05T00:00:00Z");
        code(jdbc, "SUB-30", "subscription", "999", "used", 30L, "2026-07-05T00:00:00Z");
        code(jdbc, "XY-30-B", "balance", "999", "used", 30L, "2026-06-15T00:00:00Z");
        code(jdbc, "XY-30-C", "balance", "999", "unused", null, null);
        // uid40 纯购码用户(无任何充值单):50——闲鱼老客户的典型形态
        code(jdbc, "XY-40", "balance", "50", "used", 40L, "2026-07-04T00:00:00Z");
        model = new JdbcRaffleGateReadModel(jdbc);
    }

    static void pay(JdbcTemplate j, long uid, String amount, String status, String type, String at) {
        pay(j, uid, amount, status, type, at, null);
    }

    static void pay(JdbcTemplate j, long uid, String amount, String status, String type, String at,
            String rechargeCode) {
        j.update("INSERT INTO payment_orders(user_id, order_type, status, amount, completed_at, recharge_code) "
                + "VALUES (?,?,?,?,?,?)",
                uid, type, status, new BigDecimal(amount), Timestamp.from(Instant.parse(at)), rechargeCode);
    }

    static void code(JdbcTemplate j, String codeStr, String type, String value, String status, Long usedBy,
            String usedAt) {
        j.update("INSERT INTO redeem_codes(code, type, value, status, used_by, used_at) VALUES (?,?,?,?,?,?)",
                codeStr, type, new BigDecimal(value), status, usedBy,
                usedAt == null ? null : Timestamp.from(Instant.parse(usedAt)));
    }

    static void spend(JdbcTemplate j, long uid, double cost, int billingType, String at) {
        j.update("INSERT INTO usage_logs(user_id, actual_cost, billing_type, created_at) VALUES (?,?,?,?)",
                uid, cost, billingType, Timestamp.from(Instant.parse(at)));
    }

    @Test
    void rechargeGateHonoursWindowStatusAndOrderType() {
        assertThat(model.gateValue(10, "RECHARGE", FROM, TO)).isEqualByComparingTo("100");
        assertThat(model.gateValue(20, "RECHARGE", FROM, TO)).isEqualByComparingTo("0");
    }

    @Test
    void rechargeCountsRedeemedCodesExcludingZpayMirrors() {
        assertThat(model.gateValue(30, "RECHARGE", FROM, TO)).isEqualByComparingTo("85");
        assertThat(model.gateValue(40, "RECHARGE", FROM, TO)).isEqualByComparingTo("50"); // 纯购码达标
        Map<Long, BigDecimal> values = model.gateValues(List.of(10L, 30L, 40L, 999L), "RECHARGE", FROM, TO);
        assertThat(values).containsOnlyKeys(10L, 30L, 40L);
        assertThat(values.get(30L)).isEqualByComparingTo("85");
        assertThat(values.get(40L)).isEqualByComparingTo("50");
    }

    @Test
    void spendGateCountsBalanceBilledOnly() {
        assertThat(model.gateValue(20, "SPEND", FROM, TO)).isEqualByComparingTo("5.0");
        assertThat(model.gateValue(10, "SPEND", FROM, TO)).isEqualByComparingTo("0");
    }

    @Test
    void batchGateValuesOmitUsersWithoutRows() {
        Map<Long, BigDecimal> values = model.gateValues(List.of(10L, 20L, 999L), "RECHARGE", FROM, TO);
        assertThat(values).containsOnlyKeys(10L); // 20/999 窗口内无充值流水 → 缺席
        assertThat(values.get(10L)).isEqualByComparingTo("100");
        assertThat(model.gateValues(List.of(), "RECHARGE", FROM, TO)).isEmpty(); // 空入参短路
    }

    @Test
    void unknownGateTypeRejected() {
        assertThatThrownBy(() -> model.gateValue(10, "MAGIC", FROM, TO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registeredAtsAndDisplayNamesFollowContracts() {
        assertThat(model.registeredAts(List.of(10L, 999L)))
                .containsOnlyKeys(10L)
                .containsEntry(10L, Instant.parse("2026-06-01T00:00:00Z"));
        Map<Long, String> names = model.displayNamesByIds(List.of(10L, 20L));
        assertThat(names.get(10L)).isEqualTo("老王");             // username 优先
        assertThat(names.get(20L)).isEqualTo("12***67@qq.com");   // 前2+***+后2(本地段≥5)
        assertThat(model.displayNamesByIds(List.of())).isEmpty();
    }
}
