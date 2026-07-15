package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// V11 迁移冒烟:nb_ledger 表与约束存在、EARN 方向 CHECK 生效、补铸幂等且与来源对账。
/// NB 值唯一真源 = SUM(nb_ledger.points),设计稿 ai-relay specs/2026-07-15-checkin-nb-ledger-design.md。
@SpringBootTest(classes = AchievementSchemaMigrationTestApp.class)
@Testcontainers
class NbLedgerSchemaMigrationTest {

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

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void nbLedgerTableAndConstraintsExist() {
        Integer cols = jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.columns "
                        + "WHERE table_schema='activity' AND table_name='nb_ledger'",
                Integer.class);
        assertThat(cols).isGreaterThanOrEqualTo(14);
        Integer uq = jdbc.queryForObject(
                "SELECT count(*) FROM pg_constraint WHERE conname='uq_nb_ledger_source'", Integer.class);
        assertThat(uq).isEqualTo(1);
    }

    @Test
    void earnRowsMustBePositive() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at) "
                        + "VALUES (1, 1, 'EARN', 'checkin_daily', '2026-07-15', 0, now())"))
                .hasMessageContaining("ck_nb_ledger_sign");
    }

    @Test
    void backfillIsIdempotentAndMatchesSources() {
        // 灌一行解锁源数据,重放补铸 SQL 两次:账本行数只等于源行数,点数与来源恒等
        jdbc.update("INSERT INTO activity.achievement_unlock "
                + "(id, user_id, achievement_code, unlocked_at, points_at_unlock, unlock_source) "
                + "VALUES (91001, 9001, 'checkin_first', now(), 5, 'test')");
        jdbc.update("INSERT INTO activity.checkin_record (id, user_id, checkin_date, checked_in_at) "
                + "VALUES (91002, 9001, DATE '2026-07-15', now())");
        String unlockBackfill = """
                INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at)
                SELECT u.id, u.user_id, 'EARN', 'achievement_unlock', u.achievement_code, u.points_at_unlock, u.unlocked_at
                FROM activity.achievement_unlock u
                WHERE u.revoked_at IS NULL AND u.points_at_unlock > 0
                ON CONFLICT (user_id, source_type, source_ref) DO NOTHING""";
        String checkinBackfill = """
                INSERT INTO activity.nb_ledger (id, user_id, entry_type, source_type, source_ref, points, occurred_at)
                SELECT c.id, c.user_id, 'EARN', 'checkin_daily', to_char(c.checkin_date, 'YYYY-MM-DD'), 3, c.checked_in_at
                FROM activity.checkin_record c
                ON CONFLICT (user_id, source_type, source_ref) DO NOTHING""";
        jdbc.update(unlockBackfill);
        jdbc.update(unlockBackfill);
        jdbc.update(checkinBackfill);
        jdbc.update(checkinBackfill);
        Integer rows = jdbc.queryForObject(
                "SELECT count(*) FROM activity.nb_ledger WHERE user_id=9001", Integer.class);
        assertThat(rows).isEqualTo(2);
        Integer total = jdbc.queryForObject(
                "SELECT COALESCE(SUM(points),0) FROM activity.nb_ledger WHERE user_id=9001", Integer.class);
        assertThat(total).isEqualTo(8); // 解锁 5 + 打卡 3
    }
}
