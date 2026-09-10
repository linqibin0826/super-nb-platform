package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.exception.DrawExcludedException;
import me.supernb.activity.domain.model.Campaign;
import org.junit.jupiter.api.AfterEach;
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

/// 中转接入排除:被排除的用户充够了也抽不到——单抽 / 十连都在事务里被 DrawExcludedException 拒掉,
/// 不落记录、不领槽(事务回滚)。这是服务端真闸,前端弹窗只是礼貌。
@SpringBootTest(classes = ActivityInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class DrawAdapterExclusionTest {

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

    static final long USER = 42L;
    static final Instant START = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant END = Instant.parse("2026-08-01T00:00:00Z");

    @Autowired
    DrawAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void reset() {
        ActivityInfraTestApp.excluded = false;
        ActivityInfraTestApp.recharge = new BigDecimal("150");
    }

    Campaign seed(int slots) {
        jdbc.execute("TRUNCATE activity.draw, activity.prize_slot, activity.campaign");
        Long cid = jdbc.queryForObject(
                "INSERT INTO activity.campaign (id, name, starts_at, ends_at, status, consolation_amount) "
                        + "VALUES (1, 'c', ?, ?, 'active', 5) RETURNING id",
                Long.class, java.sql.Timestamp.from(START), java.sql.Timestamp.from(END));
        for (int i = 0; i < slots; i++) {
            jdbc.update("INSERT INTO activity.prize_slot (id, campaign_id, amount, redeem_code, status) "
                    + "VALUES (?, ?, 10, ?, 'available')", 1000 + i, cid, "CODE" + i);
        }
        return new Campaign(cid, "c", START, END, "active", new BigDecimal("5"));
    }

    @Test
    void excludedUserCannotDrawEvenWithRecharge() {
        ActivityInfraTestApp.excluded = true;
        ActivityInfraTestApp.recharge = new BigDecimal("1100"); // 散客的话应得 11 次
        Campaign campaign = seed(5);

        assertThatThrownBy(() -> adapter.drawFor(campaign, USER)).isInstanceOf(DrawExcludedException.class);
        assertThatThrownBy(() -> adapter.drawAllFor(campaign, USER)).isInstanceOf(DrawExcludedException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity.prize_slot WHERE status='claimed'", Integer.class)).isZero();
        assertThat(adapter.countDraws(campaign.id(), USER)).isZero();
    }

    @Test
    void ordinaryUserStillDraws() {
        Campaign campaign = seed(5);
        assertThat(adapter.drawFor(campaign, USER).consolation()).isFalse();
        assertThat(adapter.countDraws(campaign.id(), USER)).isEqualTo(1);
    }
}
