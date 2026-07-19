package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleCampaignJpaRepository;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleEntryJpaRepository;
import me.supernb.activity.infra.adapter.persistence.dao.RafflePrizeJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleEntryEntity;
import me.supernb.activity.infra.adapter.persistence.entity.RafflePrizeEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.exception.RafflePrizeNotFoundException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// V5 迁移与 raffle 持久化基座:审计默认值、双唯一约束、CAS 抢闸语义、实体雪花 id。
@SpringBootTest(classes = ActivityInfraTestApp.class)
@Import({RafflePrizeAdapter.class, RaffleEntryAdapter.class, RaffleCampaignAdapter.class}) // 最小装配外补被测适配器
@Testcontainers
class RafflePersistenceTest {

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

    @Autowired JdbcTemplate jdbc;
    @Autowired RaffleCampaignJpaRepository campaigns;
    @Autowired RaffleEntryJpaRepository entries;
    @Autowired RafflePrizeJpaRepository prizes;
    @Autowired PlatformTransactionManager txManager;
    @Autowired RafflePrizeAdapter prizeAdapter;
    @Autowired RaffleEntryAdapter entryAdapter;
    @Autowired RaffleCampaignAdapter campaignAdapter;

    /// 造期:纯 SQL 显式 id(雪花基座无自增),审计列吃 DEFAULT。
    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE activity.raffle_entry, activity.raffle_prize, activity.raffle_campaign");
        jdbc.update("INSERT INTO activity.raffle_campaign (id, name, entry_open_at, entry_close_at, draw_at, "
                + "gate_type, gate_amount, gate_from) VALUES (1, '第一届发布会', now() - interval '1 day', "
                + "now() + interval '1 day', now() + interval '1 day', 'RECHARGE', 100, '2026-01-01')");
        jdbc.update("INSERT INTO activity.raffle_prize (id, campaign_id, tier, display_name, kind, payload, sort_order) "
                + "VALUES (1001, 1, 'S', '疯狂星期四专项(V我50)', 'ALIPAY_CODE', 'FAKE-KFC-50', 0)");
    }

    @Test
    void migrationAppliesWithAuditDefaults() {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, weight_mode, created_at, version FROM activity.raffle_campaign WHERE id = 1");
        assertThat(row.get("status")).isEqualTo("active");
        assertThat(row.get("weight_mode")).isEqualTo("EQUAL");
        assertThat(row.get("created_at")).isNotNull();
        assertThat(((Number) row.get("version")).longValue()).isZero();
    }

    @Test
    void entrySaveCarriesSnowflakeIdAndUniqueUserPerCampaign() {
        RaffleEntryEntity first = entries.save(new RaffleEntryEntity(1, 42, 1, new BigDecimal("130"), "1.2.3.4", "UA"));
        assertThat(first.getId()).isGreaterThan(1_000_000_000L); // 雪花量级
        assertThatThrownBy(() -> {
            entries.save(new RaffleEntryEntity(1, 42, 2, new BigDecimal("130"), null, null));
            entries.flush();
        }).isInstanceOf(DataIntegrityViolationException.class); // uq_raffle_entry_user
    }

    @Test
    void entryNoUniquePerCampaign() {
        entries.saveAndFlush(new RaffleEntryEntity(1, 42, 7, new BigDecimal("130"), null, null));
        assertThatThrownBy(() -> {
            entries.save(new RaffleEntryEntity(1, 43, 7, new BigDecimal("200"), null, null));
            entries.flush();
        }).isInstanceOf(DataIntegrityViolationException.class); // uq_raffle_entry_no
    }

    @Test
    void casMarkDrawnFiresExactlyOnce() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Integer first = tx.execute(s -> campaigns.markDrawn(1));
        Integer second = tx.execute(s -> campaigns.markDrawn(1));
        assertThat(first).isEqualTo(1);
        assertThat(second).isZero(); // status 已是 drawn,CAS 不再命中
        assertThat(jdbc.queryForObject("SELECT drawn_at FROM activity.raffle_campaign WHERE id = 1", Instant.class))
                .isNotNull();
    }

    @Test
    void prizeAssignRoundTrip() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        tx.executeWithoutResult(s -> {
            RafflePrizeEntity p = prizes.findByCampaignIdOrderBySortOrderAscIdAsc(1).getFirst();
            p.assign(42, Instant.parse("2026-07-13T02:30:00Z"));
        }); // 受管实体脏检查随提交落库
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT winner_user_id, assigned_at FROM activity.raffle_prize WHERE id = 1001");
        assertThat(((Number) row.get("winner_user_id")).longValue()).isEqualTo(42);
        assertThat(row.get("assigned_at")).isNotNull();
    }

    @Test
    void winsOfProjectsDrawnCampaignsOnlyWithoutPayload() {
        // 第 2 期已开奖且归属给 42;第 1 期 active 也归属给 42(不该出现在公开记录里)
        jdbc.update("INSERT INTO activity.raffle_campaign (id, name, entry_open_at, entry_close_at, "
                + "draw_at, gate_type, gate_amount, gate_from, status, drawn_at) VALUES "
                + "(2, '第二届', now() - interval '3 day', now() - interval '2 day', "
                + "now() - interval '2 day', 'RECHARGE', 100, '2026-01-01', 'drawn', now() - interval '2 day')");
        jdbc.update("INSERT INTO activity.raffle_prize (id, campaign_id, tier, display_name, kind, "
                + "payload, sort_order, winner_user_id, assigned_at) VALUES "
                + "(2001, 2, 'B', '瑞幸咖啡(9.9)', 'ALIPAY_CODE', 'FAKE-LUCKIN', 0, 42, now())");
        jdbc.update("UPDATE activity.raffle_prize SET winner_user_id = 42, assigned_at = now() WHERE id = 1001");
        List<PersonWinsView.Win> wins = prizeAdapter.winsOf(42);
        assertThat(wins).hasSize(1);
        assertThat(wins.get(0).campaignName()).isEqualTo("第二届");
        assertThat(wins.get(0).prizeDisplayName()).isEqualTo("瑞幸咖啡(9.9)");
    }

    @Test
    void findByNoResolvesPublicCoordinate() {
        jdbc.update("INSERT INTO activity.raffle_entry (id, campaign_id, user_id, entry_no, "
                + "gate_value_at_entry) VALUES (3001, 1, 42, 7, 130)");
        assertThat(entryAdapter.findByNo(1, 7)).hasValueSatisfying(e -> {
            assertThat(e.userId()).isEqualTo(42);
            assertThat(e.entryNo()).isEqualTo(7);
        });
        assertThat(entryAdapter.findByNo(1, 8)).isEmpty();
    }

    @Test
    void currentKeepsDrawnOnStageUntilNextCampaignOpens() {
        // 唯一一期 active → 是展示期
        assertThat(campaignAdapter.current()).hasValueSatisfying(c -> assertThat(c.id()).isEqualTo(1));
        // 开完(drawn)仍留在台上供迟到重放
        jdbc.update("UPDATE activity.raffle_campaign SET status = 'drawn', drawn_at = now() WHERE id = 1");
        assertThat(campaignAdapter.current()).hasValueSatisfying(c -> {
            assertThat(c.id()).isEqualTo(1);
            assertThat(c.drawn()).isTrue();
        });
        // 下一期开放 → 顶下去
        jdbc.update("INSERT INTO activity.raffle_campaign (id, name, entry_open_at, entry_close_at, "
                + "draw_at, gate_type, gate_amount, gate_from) VALUES (3, '第一期', now(), "
                + "now() + interval '1 day', now() + interval '1 day', 'RECHARGE', 100, '2026-01-01')");
        assertThat(campaignAdapter.current()).hasValueSatisfying(c -> assertThat(c.id()).isEqualTo(3));
        // cancelled 一律隐身
        jdbc.update("UPDATE activity.raffle_campaign SET status = 'cancelled' WHERE id = 3");
        assertThat(campaignAdapter.current()).hasValueSatisfying(c -> assertThat(c.id()).isEqualTo(1));
    }

    @Test
    void createPersistsActiveCampaignWithSnowflakeId() {
        long id = campaignAdapter.create("测试期", Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-08-02T00:00:00Z"), Instant.parse("2026-08-02T00:30:00Z"),
                GateType.RECHARGE, new BigDecimal("30"), Instant.parse("2026-06-01T00:00:00Z"), null,
                WeightMode.EQUAL);
        assertThat(id).isGreaterThan(1_000_000_000L); // 雪花量级,不撞种子小整数 id=1
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, name, gate_amount FROM activity.raffle_campaign WHERE id = " + id);
        assertThat(row.get("status")).isEqualTo("active");
        assertThat(row.get("name")).isEqualTo("测试期");
    }

    @Test
    void updateMutatesExistingCampaign() {
        long id = campaignAdapter.create("初版", Instant.now(), Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200), GateType.RECHARGE, new BigDecimal("20"),
                Instant.parse("2026-06-01T00:00:00Z"), null, WeightMode.EQUAL);
        campaignAdapter.update(id, "改名后", Instant.now(), Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200), GateType.RECHARGE, new BigDecimal("50"),
                Instant.parse("2026-06-01T00:00:00Z"), 7, WeightMode.EQUAL);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT name, gate_amount, min_account_age_days FROM activity.raffle_campaign WHERE id = " + id);
        assertThat(row.get("name")).isEqualTo("改名后");
        assertThat(((Number) row.get("gate_amount")).doubleValue()).isEqualTo(50.0);
        assertThat(row.get("min_account_age_days")).isEqualTo(7);
    }

    @Test
    void cancelWorksRegardlessOfCurrentStatus() {
        // 已开奖的期也能作废(彩排局 id=1 先例):种子期 id=1 先手工翻成 drawn 再作废
        jdbc.update("UPDATE activity.raffle_campaign SET status = 'drawn', drawn_at = now() WHERE id = 1");
        campaignAdapter.cancel(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM activity.raffle_campaign WHERE id = 1", String.class))
                .isEqualTo("cancelled");
    }

    @Test
    void listAllIncludesEveryStatus() {
        jdbc.update("UPDATE activity.raffle_campaign SET status = 'cancelled' WHERE id = 1");
        long freshId = campaignAdapter.create("新种子期", Instant.now(), Instant.now().plusSeconds(3600),
                Instant.now().plusSeconds(7200), GateType.RECHARGE, new BigDecimal("30"),
                Instant.parse("2026-06-01T00:00:00Z"), null, WeightMode.EQUAL);
        List<Long> ids = campaignAdapter.listAll().stream().map(c -> c.id()).toList();
        assertThat(ids).contains(1L, freshId); // cancelled 的种子期与新建的 active 期都要出现
    }

    @Test
    void updateUnknownCampaignThrowsNotFound() {
        assertThatThrownBy(() -> campaignAdapter.update(999_999_999_999L, "x", Instant.now(),
                Instant.now().plusSeconds(1), Instant.now().plusSeconds(2), GateType.RECHARGE,
                new BigDecimal("1"), Instant.now(), null, WeightMode.EQUAL))
                .isInstanceOf(RaffleNotFoundException.class);
    }

    @Test
    void createPersistsPrizeWithSnowflakeId() {
        long id = prizeAdapter.create(1, "A", "测试奖品", "ALIPAY_CODE", "PLACEHOLDER", 5);
        assertThat(id).isGreaterThan(1_000_000_000L);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT campaign_id, payload, sort_order FROM activity.raffle_prize WHERE id = " + id);
        assertThat(((Number) row.get("campaign_id")).longValue()).isEqualTo(1);
        assertThat(row.get("payload")).isEqualTo("PLACEHOLDER");
    }

    @Test
    void updateMutatesExistingPrize() {
        long id = prizeAdapter.create(1, "A", "旧名", "ALIPAY_CODE", "OLD", 5);
        prizeAdapter.update(id, "S", "新名", "ALIPAY_CODE", "NEW", 0);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT tier, display_name, payload, sort_order FROM activity.raffle_prize WHERE id = " + id);
        assertThat(row.get("tier")).isEqualTo("S");
        assertThat(row.get("payload")).isEqualTo("NEW");
    }

    @Test
    void updatePayloadOnlyTouchesPayload() {
        long id = prizeAdapter.create(1, "A", "保持不变", "ALIPAY_CODE", "", 9);
        prizeAdapter.updatePayload(id, "SuperNB7X9K2M");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT display_name, payload, sort_order FROM activity.raffle_prize WHERE id = " + id);
        assertThat(row.get("display_name")).isEqualTo("保持不变"); // 未被 updatePayload 动过
        assertThat(row.get("payload")).isEqualTo("SuperNB7X9K2M");
        assertThat(((Number) row.get("sort_order")).intValue()).isEqualTo(9);
    }

    @Test
    void deleteRemovesPrize() {
        long id = prizeAdapter.create(1, "D", "待删", "ALIPAY_CODE", "X", 99);
        prizeAdapter.delete(id);
        assertThat(jdbc.queryForList("SELECT 1 FROM activity.raffle_prize WHERE id = " + id)).isEmpty();
    }

    @Test
    void createBatchInsertsAllInOneTransaction() {
        List<Long> ids = prizeAdapter.createBatch(1, "C", "批量兑换码",
                "REDEEM_CODE", List.of("code-a", "code-b", "code-c"), 10);
        assertThat(ids).hasSize(3);
        String idList = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT payload, sort_order FROM activity.raffle_prize WHERE id IN (" + idList + ") ORDER BY sort_order");
        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).get("payload")).isEqualTo("code-a");
        assertThat(((Number) rows.get(0).get("sort_order")).intValue()).isEqualTo(10);
        assertThat(((Number) rows.get(2).get("sort_order")).intValue()).isEqualTo(12);
    }

    @Test
    void updateUnknownPrizeThrowsNotFound() {
        assertThatThrownBy(() -> prizeAdapter.update(999_999_999_999L, "x", "x", "ALIPAY_CODE", "x", 0))
                .isInstanceOf(RafflePrizeNotFoundException.class);
    }
}
