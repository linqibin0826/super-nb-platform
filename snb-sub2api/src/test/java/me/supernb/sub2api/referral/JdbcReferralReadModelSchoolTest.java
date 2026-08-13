package me.supernb.sub2api.referral;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

/// 开学季两查询:qualifiedInviteeCount(带人里程碑计数)与 topInviters(拉人榜)。
///
/// 合格被邀人口径:窗口内注册 && 人生首笔 COMPLETED 付款单 ≥min 且落窗口内;
/// 首笔金额不够,后面充再多也不算(首充语义)。榜单排序:人数降序 → 先达到者优先
/// (MAX(first_at) ASC) → inviter_id;排除站长自号(inviter_id=1)、admin、软删,脱敏出场。
@Testcontainers
class JdbcReferralReadModelSchoolTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static final Instant START = Instant.parse("2026-08-13T04:00:00Z");
    static final Instant END = Instant.parse("2026-08-31T16:00:00Z");
    static final BigDecimal MIN = new BigDecimal("30");

    static ReferralReadModel readModel;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, role TEXT, "
                + "created_at TIMESTAMPTZ, deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE user_affiliates (user_id BIGINT, inviter_id BIGINT)");
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "amount NUMERIC(20,2), order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");

        // 邀请人:10=alice、20=bob、1=站长自号(榜单排除)、30=carol(admin,榜单排除)
        user(1, "admin@x.com", "user", "2026-01-01T00:00:00Z", null);
        user(10, "alice@qq.com", "user", "2026-01-01T00:00:00Z", null);
        user(20, "bob@gmail.com", "user", "2026-01-01T00:00:00Z", null);
        user(30, "carol@x.com", "admin", "2026-01-01T00:00:00Z", null);

        // ✅ alice 的合格被邀 ×2:窗口内注册,首充 ≥30 落窗口;最后一个 08-16 到位
        inviteeWithOrder(11, 10, "2026-08-14T00:00:00Z", "30.00", "2026-08-15T00:00:00Z");
        inviteeWithOrder(12, 10, "2026-08-14T00:00:00Z", "50.00", "2026-08-16T00:00:00Z");
        // ✅ bob 的合格被邀 ×2:最后一个 08-15 就到位(与 alice 同 2 人,先达到 → 排前)
        inviteeWithOrder(21, 20, "2026-08-14T00:00:00Z", "30.00", "2026-08-14T12:00:00Z");
        inviteeWithOrder(22, 20, "2026-08-14T00:00:00Z", "30.00", "2026-08-15T00:00:00Z");
        // ❌ alice 的不合格被邀四连:
        // 首笔只有 ¥20(第二笔 ¥50 不救——首充语义)
        user(13, "13@qq.com", "user", "2026-08-14T00:00:00Z", null);
        aff(13, 10);
        order(13, "balance", "COMPLETED", "20.00", "2026-08-15T00:00:00Z");
        order(13, "balance", "COMPLETED", "50.00", "2026-08-16T00:00:00Z");
        // 窗口前注册
        inviteeWithOrder(14, 10, "2026-08-01T00:00:00Z", "30.00", "2026-08-15T00:00:00Z");
        // 首充落窗口后
        inviteeWithOrder(15, 10, "2026-08-14T00:00:00Z", "30.00", "2026-09-05T00:00:00Z");
        // 软删被邀
        user(16, "16@qq.com", "user", "2026-08-14T00:00:00Z", "2026-08-20T00:00:00Z");
        aff(16, 10);
        order(16, "balance", "COMPLETED", "30.00", "2026-08-15T00:00:00Z");
        // ✅ 订阅型付款单也算首充(carol 邀,但 carol 是 admin → 计数算 carol 的,榜单不出 carol)
        user(31, "31@qq.com", "user", "2026-08-14T00:00:00Z", null);
        aff(31, 30);
        jdbc.update("INSERT INTO payment_orders (user_id, amount, order_type, status, completed_at) "
                        + "VALUES (?,?, 'subscription', 'COMPLETED', ?)",
                31L, new BigDecimal("30.00"), Timestamp.from(Instant.parse("2026-08-15T00:00:00Z")));
        // 站长自号邀(榜单排除)
        inviteeWithOrder(51, 1, "2026-08-14T00:00:00Z", "30.00", "2026-08-15T00:00:00Z");

        readModel = new JdbcReferralReadModel(jdbc);
    }

    static void user(long id, String email, String role, String created, String deleted) {
        jdbc.update("INSERT INTO users VALUES (?,?,?,?,?)", id, email, role,
                Timestamp.from(Instant.parse(created)),
                deleted == null ? null : Timestamp.from(Instant.parse(deleted)));
    }

    static void aff(long userId, long inviterId) {
        jdbc.update("INSERT INTO user_affiliates (user_id, inviter_id) VALUES (?,?)", userId, inviterId);
    }

    static void order(long uid, String type, String status, String amount, String completedAt) {
        jdbc.update("INSERT INTO payment_orders (user_id, amount, order_type, status, completed_at) "
                        + "VALUES (?,?,?,?,?)",
                uid, new BigDecimal(amount), type, status,
                completedAt == null ? null : Timestamp.from(Instant.parse(completedAt)));
    }

    /// 窗口内注册 + 单笔 COMPLETED balance 首充的合格被邀样板。
    static void inviteeWithOrder(long id, long inviter, String created, String amount, String chargedAt) {
        user(id, id + "00001@qq.com", "user", created, null);
        aff(id, inviter);
        order(id, "balance", "COMPLETED", amount, chargedAt);
    }

    @Test
    void qualifiedInviteeCountAppliesFullRules() {
        // alice:11/12 合格;13(首笔<30)/14(窗口前注册)/15(首充窗口后)/16(软删) 全排除
        assertThat(readModel.qualifiedInviteeCount(10L, START, END, MIN)).isEqualTo(2);
    }

    @Test
    void subscriptionOrderCountsAsFirstCharge() {
        assertThat(readModel.qualifiedInviteeCount(30L, START, END, MIN)).isEqualTo(1);
    }

    @Test
    void topInvitersOrdersByCountThenEarliestReachAndMasks() {
        List<ReferralReadModel.InviterRank> top = readModel.topInviters(START, END, MIN, 10);
        // bob 与 alice 同 2 人,bob 08-15 先凑齐 → 排前;carol(admin)/站长自号不出榜
        assertThat(top).hasSize(2);
        assertThat(top.get(0).name()).isEqualTo("***@gmail.com");
        assertThat(top.get(0).count()).isEqualTo(2);
        assertThat(top.get(1).name()).isEqualTo("a***e@qq.com");
        assertThat(top.get(1).count()).isEqualTo(2);
    }

    @Test
    void topInvitersRespectsLimit() {
        assertThat(readModel.topInviters(START, END, MIN, 1)).hasSize(1);
    }
}
