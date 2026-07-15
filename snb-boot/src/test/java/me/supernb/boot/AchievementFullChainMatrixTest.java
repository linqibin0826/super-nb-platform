package me.supernb.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.supernb.activity.app.usecase.achievement.AccountAnniversaryJudgeJob;
import me.supernb.activity.app.usecase.achievement.AchievementJudgeEngine;
import me.supernb.activity.app.usecase.achievement.CheckinMetricSyncJob;
import me.supernb.activity.app.usecase.achievement.GalleryMetricSyncJob;
import me.supernb.activity.app.usecase.achievement.LeaderboardMetricSyncJob;
import me.supernb.activity.app.usecase.achievement.RaffleGateMetricSyncJob;
import me.supernb.activity.app.usecase.achievement.RechargeAchievementJudgeJob;
import me.supernb.activity.app.usecase.achievement.ReferralMetricSyncJob;
import me.supernb.activity.app.usecase.achievement.UsageMetricSyncJob;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.gallery.domain.port.storage.ImageStoragePort;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 42 条成就全链路矩阵:每条成就一个专属用户,从**源表**灌数(sub2api 桩表按生产真实列形状建、
/// activity/gallery 表由真实 Flyway 迁移建)→ 依序跑**真实生产者 job**(不 mock 任何一层)→
/// 真实判定引擎 → 断言 achievement_unlock 真实落库。收尾三重把关:①每码逐一断言;②阴性对照
/// (未达阈值/lte 越界不解锁);③全覆盖断言(矩阵用户解锁码并集 == 目录全部 42 码,防"漏测某码
/// 而不自知")。签到 job 的两个包内可见入口经 ReflectionTestUtils 调用(生产入口是 @Scheduled
/// 日频/月频 cron,测试内不可等)。
@SpringBootTest
@Testcontainers
class AchievementFullChainMatrixTest {

    @Test
    void ledgerReconcilesWithUnlockFacts() {
        // 总账对账(NB 账本口径):成就侧账本行与解锁事实逐用户恒等——行数与点数和都不允许偏差;
        // 矩阵已让 42 码全部真实解锁,此断言证明「解锁即记账」在全部解锁路径(引擎/直判 job)下无一漏记
        Integer mismatch = jdbc.queryForObject("""
                SELECT count(*) FROM (
                  SELECT u.user_id,
                         SUM(u.points_at_unlock) AS expect_points,
                         (SELECT COALESCE(SUM(l.points),0) FROM activity.nb_ledger l
                           WHERE l.user_id = u.user_id AND l.source_type = 'achievement_unlock') AS ledger_points
                  FROM activity.achievement_unlock u
                  WHERE u.revoked_at IS NULL
                  GROUP BY u.user_id) t
                WHERE t.expect_points <> t.ledger_points""", Integer.class);
        assertThat(mismatch).isZero();
    }


    private static final ZoneId SH = ZoneId.of("Asia/Shanghai");

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("sub2api.read-datasource.url", PG::getJdbcUrl);
        r.add("sub2api.read-datasource.username", PG::getUsername);
        r.add("sub2api.read-datasource.password", PG::getPassword);
        r.add("activity.checkin.scan-enabled", () -> "true");
        r.add("activity.checkin.launch-date", () -> "2026-07-13"); // 钉死创始月=2026-07,绝版链路可复验
    }

    @MockitoBean
    ImageStoragePort imageStorage; // gallery 生成 Handler 依赖,矩阵不触存储

    @Autowired UsageMetricSyncJob usageJob;
    @Autowired CheckinMetricSyncJob checkinJob;
    @Autowired GalleryMetricSyncJob galleryJob;
    @Autowired RaffleGateMetricSyncJob raffleGateJob;
    @Autowired LeaderboardMetricSyncJob leaderboardJob;
    @Autowired ReferralMetricSyncJob referralJob;
    @Autowired RechargeAchievementJudgeJob rechargeJudge;
    @Autowired AccountAnniversaryJudgeJob anniversaryJudge;
    @Autowired AchievementJudgeEngine engine;
    @Autowired AchievementUnlockPort unlockPort;
    @Autowired JdbcTemplate jdbc;

    @Test
    void everyAchievementUnlocksThroughItsRealPipeline() {
        createSub2apiStubTables();
        seedAllAxes();

        // —— 依序跑真实生产者(顺序有依赖:referral 交叉读 usage 写出的被邀者 metric)——
        usageJob.syncHourly();
        usageJob.syncDailyPeak();
        LocalDate today = LocalDate.now(SH);
        ReflectionTestUtils.invokeMethod(checkinJob, "syncDailyAt", today);
        for (YearMonth m : List.of(YearMonth.of(2026, 4), YearMonth.of(2026, 5), YearMonth.of(2026, 6),
                YearMonth.of(2026, 7))) {
            ReflectionTestUtils.invokeMethod(checkinJob, "syncMonthlyFor", m);
        }
        galleryJob.syncDaily();
        raffleGateJob.syncDaily();
        leaderboardJob.syncDaily();
        referralJob.syncDaily();
        rechargeJudge.judgeDaily();
        anniversaryJudge.judgeDaily();
        engine.judgeHourly();

        // —— 期望矩阵:user -> 必须解锁的 code 集 ——
        Map<Long, Set<String>> expect = new LinkedHashMap<>();
        expect.put(1101L, Set.of("checkin_first", "checkin_cum_1"));
        expect.put(1102L, Set.of("checkin_cum_2"));
        expect.put(1103L, Set.of("checkin_cum_3"));
        expect.put(1104L, Set.of("midnight_courier"));
        expect.put(1105L, Set.of("ghost_return"));
        expect.put(1106L, Set.of("checkin_full_1"));
        expect.put(1107L, Set.of("checkin_full_2"));
        expect.put(1108L, Set.of("exclusive_founding_issue", "exclusive_founding_fullmonth"));
        expect.put(1109L, Set.of("api_first_call"));
        expect.put(1110L, Set.of("api_calls_1"));
        expect.put(1111L, Set.of("api_calls_2"));
        expect.put(1112L, Set.of("api_calls_3", "meta_series_master"));
        expect.put(1113L, Set.of("api_daily_peak_1"));
        expect.put(1114L, Set.of("late_night_room"));
        expect.put(1115L, Set.of("cross_surface_user"));
        expect.put(1116L, Set.of("image_gen_1"));
        expect.put(1117L, Set.of("image_gen_2"));
        expect.put(1118L, Set.of("image_gen_3"));
        expect.put(1119L, Set.of("appreciation_1"));
        expect.put(1120L, Set.of("appreciation_2"));
        expect.put(1121L, Set.of("raffle_entry_1"));
        expect.put(1122L, Set.of("raffle_win_1"));
        expect.put(1123L, Set.of("raffle_companion_1"));
        expect.put(1124L, Set.of("raffle_companion_2"));
        expect.put(1125L, Set.of("gate_ticket_1"));
        expect.put(1126L, Set.of("drawcard_1"));
        expect.put(1127L, Set.of("drawcard_2"));
        expect.put(1128L, Set.of("leaderboard_1"));
        expect.put(1129L, Set.of("leaderboard_1", "leaderboard_2"));
        expect.put(1130L, Set.of("referral_1"));
        expect.put(1131L, Set.of("referral_2"));
        expect.put(1132L, Set.of("recharge_amount_1"));
        expect.put(1133L, Set.of("recharge_amount_2")); // 全口径:¥1 现金 + ¥120 兑换码,缺码必不达标
        expect.put(1134L, Set.of("recharge_amount_3"));
        expect.put(1135L, Set.of("recharge_consistency_1"));
        expect.put(1136L, Set.of("account_anniv_1"));
        expect.put(1137L, Set.of("account_anniv_2"));
        expect.put(1140L, Set.of("meta_regular", "meta_category_onboarding"));

        SoftAssertions softly = new SoftAssertions();
        Set<String> unionUnlocked = new HashSet<>();
        for (var e : expect.entrySet()) {
            Set<String> actual = unlockPort.unlockedCodes(e.getKey());
            unionUnlocked.addAll(actual);
            for (String code : e.getValue()) {
                softly.assertThat(actual)
                        .as("user %d 应经真实链路解锁 %s(实际=%s)", e.getKey(), code, actual)
                        .contains(code);
            }
        }
        // —— 阴性对照:未达阈值/lte 越界绝不能解锁 ——
        softly.assertThat(unlockPort.unlockedCodes(1150)).as("6 天不该解锁 cum_1(阈值 7)")
                .contains("checkin_first").doesNotContain("checkin_cum_1");
        softly.assertThat(unlockPort.unlockedCodes(1128)).as("名次 30 不该解锁 lb_2(lte 10)")
                .doesNotContain("leaderboard_2");
        softly.assertThat(unlockPort.unlockedCodes(1132)).as("¥5 不该解锁 amount_2(阈值 100)")
                .doesNotContain("recharge_amount_2");
        // —— 全覆盖:矩阵解锁码并集 == 目录全部 42 码 ——
        List<String> allCodes = jdbc.queryForList(
                "SELECT code FROM activity.achievement_definition ORDER BY sort_order", String.class);
        softly.assertThat(allCodes).hasSize(42);
        softly.assertThat(unionUnlocked).as("目录里每一码都必须被矩阵证明可解锁")
                .containsAll(allCodes);
        softly.assertAll();
    }

    /// sub2api 桩表:列形状照抄生产 schema(RECHARGE 全口径 SQL 依赖 redeem_codes 的
    /// used_by/value/type/status/used_at 与 payment_orders.recharge_code 镜像剔除列)。
    private void createSub2apiStubTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS users (id BIGINT PRIMARY KEY, created_at TIMESTAMPTZ NOT NULL, "
                + "deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS payment_orders (id BIGSERIAL PRIMARY KEY, user_id BIGINT, "
                + "order_type TEXT, status TEXT, amount NUMERIC(12,2), completed_at TIMESTAMPTZ, "
                + "recharge_code TEXT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS redeem_codes (id BIGSERIAL PRIMARY KEY, code TEXT UNIQUE, "
                + "used_by BIGINT, value NUMERIC(12,2), type TEXT, status TEXT, used_at TIMESTAMPTZ, "
                + "expires_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS usage_logs (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL, "
                + "created_at TIMESTAMPTZ NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_affiliates (user_id BIGINT PRIMARY KEY, inviter_id BIGINT)");
    }

    private void seedAllAxes() {
        LocalDate today = LocalDate.now(SH);

        // —— 签到轴(checkin_record 由 Flyway V8 真实建表)——
        seedCheckinRun(1101, today, 7);
        seedCheckinRun(1102, today, 30);
        seedCheckinRun(1103, today, 100);
        seedCheckinRun(1150, today, 6); // 阴性对照:差一天
        // 零点信使:今天 00:00:30(Asia/Shanghai)
        jdbc.update("INSERT INTO activity.checkin_record (id, user_id, checkin_date, checked_in_at) "
                        + "VALUES (?, 1104, ?, ?)", 1104_000L, today,
                java.sql.Timestamp.from(today.atStartOfDay(SH).plusSeconds(30).toInstant()));
        // 诈尸打卡:今天 + 40 天前
        seedCheckinDay(1105, 1105_001L, today);
        seedCheckinDay(1105, 1105_002L, today.minusDays(40));
        // 满勤 ×1(2026-06 全月 30 天)与 ×3(2026-04/05/06 三个整月)
        seedFullMonth(1106, YearMonth.of(2026, 6));
        seedFullMonth(1107, YearMonth.of(2026, 4));
        seedFullMonth(1107, YearMonth.of(2026, 5));
        seedFullMonth(1107, YearMonth.of(2026, 6));
        // 创始月绝版:上线日 2026-07-13 起至月末 19 天全勤
        LocalDate launch = LocalDate.of(2026, 7, 13);
        for (int i = 0; i < 19; i++) {
            seedCheckinDay(1108, 1108_000L + i, launch.plusDays(i));
        }

        // —— 用量轴(usage_logs 桩表,真实小时增量/日终峰值管线)——
        seedUsage(1109, 1);
        seedUsage(1110, 100);
        seedUsage(1111, 1000);
        seedUsage(1112, 10000);
        // 日终峰值:今天(上海自然日)100 条——锚在当日 00:05,当日任何时刻运行都可数到
        jdbc.update("INSERT INTO usage_logs (user_id, created_at) "
                + "SELECT 1113, (date_trunc('day', now() AT TIME ZONE 'Asia/Shanghai') + interval '5 minutes') "
                + "AT TIME ZONE 'Asia/Shanghai' FROM generate_series(1, 100)");
        // 深夜机房:最近一个"上海 02:00"时刻(若距今 <31 分钟——即测试恰在凌晨两点边缘运行——退一天)
        jdbc.update("INSERT INTO usage_logs (user_id, created_at) SELECT 1114, "
                + "CASE WHEN ts > now() - interval '31 minutes' THEN ts - interval '1 day' ELSE ts END FROM "
                + "(SELECT (date_trunc('day', now() AT TIME ZONE 'Asia/Shanghai') + interval '2 hours') "
                + "AT TIME ZONE 'Asia/Shanghai' AS ts) t");
        // 全栈选手:文本 1 条 + 生图 1 条(合成指标双轴)
        seedUsage(1115, 1);
        seedGeneration(1115, 1115_000L, 1, "done");

        // —— 造像轴(gallery.* 由 Flyway 真实建表;like/favorite 依赖父 prompt 行)——
        seedGeneration(1116, 1116_000L, 1, "done");
        seedGeneration(1116, 1116_900L, 1, "error"); // error 不计数(链路内隐含验证)
        seedGeneration(1117, 1117_000L, 20, "done");
        seedGeneration(1118, 1118_000L, 100, "done");
        // uk_like_prompt_user:一人对同一提示词只能赞/藏一次 → 满 N 需要 N 个不同提示词
        jdbc.update("INSERT INTO gallery.prompt (id, source, source_id, title, prompt_text) "
                + "SELECT 900000 + g, 'own', 'matrix-' || g, '矩阵占位', 'matrix prompt' "
                + "FROM generate_series(1, 100) g");
        seedLikesFavs(1119, 1119_000L, 12, 8);
        seedLikesFavs(1120, 1120_000L, 60, 40);

        // —— 联动轴(raffle/gate/draw 表由 Flyway 真实建表)——
        seedRaffleCampaign(901, "drawn");
        seedRaffleCampaign(902, "drawn");
        seedRaffleCampaign(903, "drawn");
        seedRaffleCampaign(904, "drawn");
        seedRaffleCampaign(905, "drawn");
        seedRaffleCampaign(906, "active");
        seedRaffleEntry(1121_000L, 906, 1121); // 报名(active 期次即可计 entry)
        seedRaffleEntry(1122_000L, 901, 1122); // 中奖
        jdbc.update("INSERT INTO activity.raffle_prize (id, campaign_id, tier, display_name, kind, payload, "
                + "winner_user_id) VALUES (1122_100, 901, 'S', '矩阵奖', 'ALIPAY_CODE', 'x', 1122)");
        seedRaffleEntry(1123_000L, 901, 1123); // 陪跑 ×1
        for (int i = 0; i < 5; i++) { // 陪跑 ×5
            seedRaffleEntry(1124_000L + i, 901 + i, 1124);
        }
        jdbc.update("INSERT INTO activity.gate_attempt (id, user_id, attempt_date, won) "
                + "VALUES (1125000, 1125, ?, true)", today);
        jdbc.update("INSERT INTO activity.campaign (id, name, starts_at, ends_at) "
                + "VALUES (900, '矩阵期次', now() - interval '10 days', now() + interval '10 days')");
        seedDraws(1126, 1126_000L, 1);
        seedDraws(1127, 1127_000L, 10);
        jdbc.update("INSERT INTO activity.leaderboard_rank_snapshot VALUES (?, 'day', 'tokens', 1128, 30)", today);
        jdbc.update("INSERT INTO activity.leaderboard_rank_snapshot VALUES (?, 'day', 'tokens', 1129, 5)", today);

        // —— 拉新轴(user_affiliates × 被邀者 api metric 交叉)——
        seedInvitee(2130, 1130);
        seedInvitee(2131, 1131);
        seedInvitee(2132, 1131);
        seedInvitee(2133, 1131);

        // —— 充值轴(直判全口径:payment_orders ∪ redeem_codes)——
        seedPayment(1132, "5", "now() - interval '1 hour'");
        seedPayment(1133, "1", "now() - interval '1 hour'"); // ¥1 只作候选发现,达标全靠兑换码
        jdbc.update("INSERT INTO redeem_codes (code, used_by, value, type, status, used_at) "
                + "VALUES ('MATRIX-1133', 1133, 120, 'balance', 'used', now() - interval '10 days')");
        seedPayment(1134, "600", "now() - interval '1 hour'");
        seedPayment(1135, "10", "now() - interval '1 hour'");
        seedPayment(1135, "10", "date_trunc('month', now()) - interval '15 days'");
        seedPayment(1135, "10", "date_trunc('month', now()) - interval '45 days'");

        // —— 工龄轴(恰好满 N 天,上海正午防时区边界)——
        seedUser(1136, today.minusDays(100));
        seedUser(1137, today.minusDays(365));

        // —— 元编年史组合用户:一轮内凑满 10 枚 + 入职档案齐 ——
        seedCheckinRun(1140, today, 7);          // checkin_first + cum_1
        seedUsage(1140, 100);                     // api_first_call + api_calls_1
        seedGeneration(1140, 1140_500L, 20, "done"); // image_gen_1 + image_gen_2
        seedLikesFavs(1140, 1140_700L, 20, 0);    // appreciation_1
        seedRaffleEntry(1140_000L, 906, 1140);    // raffle_entry_1
        seedDraws(1140, 1140_800L, 1);            // drawcard_1  → 合计 10 枚
    }

    // ===== 造数小工具(全部打向真实表) =====

    private void seedCheckinRun(long userId, LocalDate endDay, int days) {
        for (int i = 0; i < days; i++) {
            seedCheckinDay(userId, userId * 1000 + i, endDay.minusDays(i));
        }
    }

    private void seedCheckinDay(long userId, long id, LocalDate day) {
        jdbc.update("INSERT INTO activity.checkin_record (id, user_id, checkin_date, checked_in_at) "
                        + "VALUES (?, ?, ?, ?)", id, userId, day,
                java.sql.Timestamp.from(day.atStartOfDay(SH).plusHours(12).toInstant()));
    }

    private void seedFullMonth(long userId, YearMonth month) {
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            seedCheckinDay(userId, userId * 10000 + month.getMonthValue() * 100 + d, month.atDay(d));
        }
    }

    private void seedUsage(long userId, int count) {
        jdbc.update("INSERT INTO usage_logs (user_id, created_at) "
                + "SELECT ?, now() - interval '10 minutes' FROM generate_series(1, ?)", userId, count);
    }

    private void seedGeneration(long userId, long idBase, int count, String status) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO gallery.generation (id, user_id, prompt, size, n, quality, status) "
                    + "VALUES (?, ?, 'matrix', '1024x1024', 1, 'standard', ?)", idBase + i, userId, status);
        }
    }

    private void seedLikesFavs(long userId, long idBase, int likes, int favs) {
        for (int i = 0; i < likes; i++) {
            jdbc.update("INSERT INTO gallery.prompt_like (id, prompt_id, user_id) VALUES (?, ?, ?)",
                    idBase + i, 900001 + i, userId);
        }
        for (int i = 0; i < favs; i++) {
            jdbc.update("INSERT INTO gallery.prompt_favorite (id, prompt_id, user_id) VALUES (?, ?, ?)",
                    idBase + 500 + i, 900001 + i, userId);
        }
    }

    private void seedRaffleCampaign(long id, String status) {
        jdbc.update("INSERT INTO activity.raffle_campaign (id, name, entry_open_at, entry_close_at, draw_at, "
                + "gate_type, gate_amount, gate_from, status) VALUES (?, '矩阵期次', now() - interval '5 days', "
                + "now() - interval '1 day', now() - interval '1 hour', 'RECHARGE', 0, now() - interval '30 days', ?)",
                id, status);
    }

    private void seedRaffleEntry(long id, long campaignId, long userId) {
        jdbc.update("INSERT INTO activity.raffle_entry (id, campaign_id, user_id, entry_no, gate_value_at_entry) "
                + "VALUES (?, ?, ?, ?, 0)", id, campaignId, userId, (int) (id % 100000));
    }

    private void seedDraws(long userId, long idBase, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.update("INSERT INTO activity.draw (id, campaign_id, user_id, amount) VALUES (?, 900, ?, 1)",
                    idBase + i, userId);
        }
    }

    private void seedInvitee(long inviteeId, long inviterId) {
        jdbc.update("INSERT INTO users (id, created_at) VALUES (?, now() - interval '5 days')", inviteeId);
        jdbc.update("INSERT INTO user_affiliates (user_id, inviter_id) VALUES (?, ?)", inviteeId, inviterId);
        seedUsage(inviteeId, 1); // 有效邀请口径:被邀者有首次成功调用
    }

    private void seedPayment(long userId, String amount, String completedAtSqlExpr) {
        jdbc.update("INSERT INTO payment_orders (user_id, order_type, status, amount, completed_at) "
                + "VALUES (?, 'balance', 'COMPLETED', " + amount + ", " + completedAtSqlExpr + ")", userId);
    }

    private void seedUser(long userId, LocalDate registeredDay) {
        jdbc.update("INSERT INTO users (id, created_at) VALUES (?, ?)", userId,
                java.sql.Timestamp.from(registeredDay.atStartOfDay(SH).plusHours(12).toInstant()));
    }
}
