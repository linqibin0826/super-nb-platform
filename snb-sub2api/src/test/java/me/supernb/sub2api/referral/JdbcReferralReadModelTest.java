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

@Testcontainers
class JdbcReferralReadModelTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static final Instant START = Instant.parse("2026-07-09T14:00:00Z");
    static final Instant END = Instant.parse("2026-07-16T16:00:00Z");
    static final BigDecimal CAP = new BigDecimal("288");
    static final long GROUP = 77;

    static ReferralReadModel readModel;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, email TEXT, role TEXT, "
                + "created_at TIMESTAMPTZ, deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE user_affiliates (user_id BIGINT, inviter_id BIGINT)");
        jdbc.execute("CREATE TABLE user_subscriptions (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "group_id BIGINT, deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "amount NUMERIC(20,2), order_type TEXT, status TEXT, completed_at TIMESTAMPTZ)");

        // 邀请人:10=alice(qq),20=bob(gmail);1=admin(站长自号,须排除);30=carol(role=admin 须排除)
        user(jdbc, 1, "admin@x.com", "user", "2026-01-01T00:00:00Z", null);
        user(jdbc, 10, "alice@qq.com", "user", "2026-01-01T00:00:00Z", null);
        user(jdbc, 20, "bob@gmail.com", "user", "2026-01-01T00:00:00Z", null);
        user(jdbc, 30, "carol@x.com", "admin", "2026-01-01T00:00:00Z", null);
        // 被邀请新用户(窗口内注册)
        user(jdbc, 101, "101@qq.com", "user", "2026-07-10T00:00:00Z", null);  // alice 邀
        user(jdbc, 102, "102@qq.com", "user", "2026-07-11T00:00:00Z", null);  // alice 邀
        user(jdbc, 201, "201@qq.com", "user", "2026-07-10T00:00:00Z", null);  // bob 邀
        user(jdbc, 900, "900@qq.com", "user", "2026-06-01T00:00:00Z", null);  // 窗口外注册(排除)
        user(jdbc, 910, "910@qq.com", "user", "2026-07-10T00:00:00Z", "2026-07-11T00:00:00Z"); // 软删

        aff(jdbc, 101, 10);
        aff(jdbc, 102, 10);
        aff(jdbc, 201, 20);
        aff(jdbc, 900, 20);   // bob 邀但窗口外注册
        aff(jdbc, 910, 30);   // carol(admin)邀,且被邀人软删
        aff(jdbc, 500, 1);    // 站长自号邀(inviter=1 排除)

        // 充值:alice 的被邀 101 充 200+150=350(封顶后 288);102 充 40 → alice total=390,capped=288
        //       bob 的被邀 201 充 100 → bob total=100
        order(jdbc, 101, "200", "2026-07-10T01:00:00Z");
        order(jdbc, 101, "150", "2026-07-12T01:00:00Z");
        order(jdbc, 102, "40", "2026-07-11T01:00:00Z");
        order(jdbc, 201, "100", "2026-07-11T01:00:00Z");
        order(jdbc, 900, "999", "2026-07-11T01:00:00Z"); // 900 窗口外注册,不计入 bob

        // 开通新人组(group 77):alice 的 101/102 都开;bob 的 201 开;另有软删订阅也算(曾开通)
        sub(jdbc, 101, 77, null);
        sub(jdbc, 102, 77, null);
        sub(jdbc, 201, 77, "2026-07-13T00:00:00Z"); // 组过期软删,仍算曾开通
        sub(jdbc, 101, 99, null);                   // 别的组,不计入 77 榜

        readModel = new JdbcReferralReadModel(jdbc);
    }

    static void user(JdbcTemplate j, long id, String email, String role, String created, String deleted) {
        j.update("INSERT INTO users VALUES (?,?,?,?,?)", id, email, role,
                Timestamp.from(Instant.parse(created)),
                deleted == null ? null : Timestamp.from(Instant.parse(deleted)));
    }

    static void aff(JdbcTemplate j, long userId, long inviterId) {
        j.update("INSERT INTO user_affiliates (user_id, inviter_id) VALUES (?,?)", userId, inviterId);
    }

    static void order(JdbcTemplate j, long uid, String amount, String at) {
        j.update("INSERT INTO payment_orders (user_id, amount, order_type, status, completed_at) "
                + "VALUES (?,?, 'balance', 'COMPLETED', ?)", uid, new BigDecimal(amount),
                Timestamp.from(Instant.parse(at)));
    }

    static void sub(JdbcTemplate j, long uid, long gid, String deleted) {
        j.update("INSERT INTO user_subscriptions (user_id, group_id, deleted_at) VALUES (?,?,?)",
                uid, gid, deleted == null ? null : Timestamp.from(Instant.parse(deleted)));
    }

    @Test
    void rechargeBoardSumsByInviterCapsAndMasks() {
        List<ReferralReadModel.RechargeRow> board = readModel.rechargeBoard(START, END, CAP, 3);
        assertThat(board).hasSize(2);
        // alice 原始 390(200+150+40),排第一;capped=288
        assertThat(board.get(0).name()).isEqualTo("al***@qq.com");
        assertThat(board.get(0).total()).isEqualByComparingTo("390");
        assertThat(board.get(0).capped()).isEqualByComparingTo("288");
        // bob 原始 100(仅 201,900 窗口外不算);capped=100
        assertThat(board.get(1).name()).isEqualTo("bo***@gmail.com");
        assertThat(board.get(1).total()).isEqualByComparingTo("100");
        assertThat(board.get(1).capped()).isEqualByComparingTo("100");
    }

    @Test
    void inviteBoardCountsEverOpenedGroupExcludingAdminAndSelf() {
        List<ReferralReadModel.InviteRow> board = readModel.inviteBoard(GROUP, 3);
        assertThat(board).hasSize(2);
        // alice 开了 101/102 两个 → 2;bob 开了 201(软删仍算) → 1
        assertThat(board.get(0).name()).isEqualTo("al***@qq.com");
        assertThat(board.get(0).count()).isEqualTo(2);
        assertThat(board.get(1).name()).isEqualTo("bo***@gmail.com");
        assertThat(board.get(1).count()).isEqualTo(1);
    }
}
