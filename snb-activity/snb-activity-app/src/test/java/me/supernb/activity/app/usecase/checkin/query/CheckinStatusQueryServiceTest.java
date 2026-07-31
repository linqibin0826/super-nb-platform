package me.supernb.activity.app.usecase.checkin.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.CheckinEntryGateChecker;
import me.supernb.activity.app.usecase.checkin.config.CheckinBalanceProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinEntryGateProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinSettlementProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinMilestoneView;
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort.RechargeEvent;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.nb.NbLedgerPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// 状态查询装配:字段形状按前端接线计划契约核对——eligible/ineligibleReason、
/// checkedDays 为"日"整数、里程碑成品文案、补给三档 state+statusText。
/// gaugePct 按 2026-07-14 控制器裁决采用分段刻度公式(刻度 0/A/B/C 立于 0%/33%/66%/100%,
/// 段内线性,每段另封顶到刻度线之下以消除"满格却未达标"矛盾),覆盖 spec 草稿"朝下一档线性"
/// 公式——¥36 → 43 是与前端契约示例 JSON 对齐的锚点断言。满勤里程碑(`buildMilestones`)
/// 另用固定 `LocalDate` 直接单测(包私有静态纯函数,仿 `BoardPeriods` 先例),覆盖"上线日落在
/// 被测月中旬"这一 launchDate 恒早于 today 的既有测试永远碰不到的分支(2026-07-14 复审补测)。
class CheckinStatusQueryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CheckinPort checkinPort = mock(CheckinPort.class);
    private final CheckinRechargeReadPort rechargePort = mock(CheckinRechargeReadPort.class);
    private final AccountRegistrationReadPort registrationPort = mock(AccountRegistrationReadPort.class);
    private final NbLedgerPort nbLedger = mock(NbLedgerPort.class);
    private final CheckinProperties props = new CheckinProperties("2020-01-01", 3);
    private final CheckinTierProperties tierProps = new CheckinTierProperties(
            new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("500"),
            27L, 65L, 71L, new BigDecimal("0.9"), new BigDecimal("1.9"), new BigDecimal("4.4"), 20);
    private final CheckinDailyRewardPort dailyRewardPort = mock(CheckinDailyRewardPort.class);
    private final CheckinBalanceProperties balanceProps =
            new CheckinBalanceProperties(true, "0.1", "30", "3000", 1);
    private final CheckinStatusQueryService service = serviceWith(balanceProps,
            new CheckinEntryGateProperties(false, 30, "30"));

    /// 组装被测服务(准入闸判定器用真实现+mock 端口,默认闸关保住全部既有用例的旧行为)。
    private CheckinStatusQueryService serviceWith(CheckinBalanceProperties bal,
            CheckinEntryGateProperties gateProps) {
        return new CheckinStatusQueryService(checkinPort, rechargePort, registrationPort, nbLedger, props,
                tierProps,
                new CheckinSettlementProperties(new BigDecimal("250"), new BigDecimal("10"), true, true, 20),
                dailyRewardPort, bal, new CheckinEntryGateChecker(rechargePort, gateProps));
    }

    @Test
    void eligibleUserSeesFullStatusWithProgressTierB() {
        LocalDate today = LocalDate.now(ZONE);
        when(registrationPort.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        when(checkinPort.checkedInOn(eq(42L), any())).thenReturn(false);
        when(checkinPort.datesInRange(eq(42L), any(), any()))
                .thenReturn(List.of(today.minusDays(1), today.minusDays(2)));
        when(checkinPort.totalCheckins(42)).thenReturn(37);
        when(checkinPort.countInRange(eq(42L), any(), any())).thenReturn(2);
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(new BigDecimal("36"));

        CheckinStatusView view = service.status(42);

        assertThat(view.eligible()).isTrue();
        assertThat(view.ineligibleReason()).isNull();
        assertThat(view.punchedToday()).isFalse();
        assertThat(view.todayDay()).isEqualTo(today.getDayOfMonth());
        assertThat(view.checkedDays()).containsExactly(
                today.minusDays(2).getDayOfMonth(), today.minusDays(1).getDayOfMonth());
        assertThat(view.cumulativeDays()).isEqualTo(37);
        assertThat(view.milestones()).hasSize(4);
        assertThat(view.supply().monthlyRechargeCny()).isEqualByComparingTo("36");
        assertThat(view.supply().tiers().get(0).state()).isEqualTo("armed");   // A(30)已达标
        assertThat(view.supply().tiers().get(1).state()).isEqualTo("progress"); // B(50)是下一档
        assertThat(view.supply().tiers().get(1).statusText()).isEqualTo("差 ¥14");
        assertThat(view.supply().tiers().get(2).state()).isEqualTo("dim");     // C(500)远未达标
        // 控制器裁决锚点:33+(36-30)/(50-30)×33=42.9→43,与前端契约示例 JSON 吻合
        assertThat(view.supply().gaugePct()).isEqualTo(43);
    }

    @Test
    void tooYoungAccountIsIneligibleAndZeroTierIsProgress() {
        when(registrationPort.registeredAt(1)).thenReturn(Optional.empty());
        when(checkinPort.checkedInOn(eq(1L), any())).thenReturn(false);
        when(checkinPort.datesInRange(eq(1L), any(), any())).thenReturn(List.of());
        when(checkinPort.totalCheckins(1)).thenReturn(0);
        when(checkinPort.countInRange(eq(1L), any(), any())).thenReturn(0);
        when(rechargePort.monthlyRecharge(eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);

        CheckinStatusView view = service.status(1);

        assertThat(view.eligible()).isFalse();
        assertThat(view.ineligibleReason()).isEqualTo("account_too_new");
        assertThat(view.milestones()).allSatisfy(m -> assertThat(m.achieved()).isFalse());
        assertThat(view.supply().tiers().get(0).state()).isEqualTo("progress"); // 0 元时 A 是下一档
        assertThat(view.supply().gaugePct()).isZero();
    }

    /// 连签阶梯用到的两个新端口给中性默认值,免得每个既有用例都得重复 stub
    /// (未 stub 的 BigDecimal 返回 null,会在 compareTo 处 NPE)。需要具体值的用例自行覆盖。
    @BeforeEach
    void stubDailyRewardDefaults() {
        when(rechargePort.lifetimeRecharge(anyLong(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.myMonthlyBalanceTotal(anyLong(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.findByUserAndDay(anyLong(), any())).thenReturn(Optional.empty());
    }

    @Test
    void dailyRewardShowsTodayAndTomorrowLadder() {
        // 本月连签到昨天 6 天、今天已签 → 今天第 7 天 ¥0.70/21NB,明天第 8 天 ¥0.80/24NB
        LocalDate day = LocalDate.of(2026, 8, 10);
        stubEligibleUserOnDate(day, 7, true);
        when(rechargePort.lifetimeRecharge(eq(42L), any())).thenReturn(new BigDecimal("100"));
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.myMonthlyBalanceTotal(eq(42L), any(), any())).thenReturn(new BigDecimal("2.10"));
        when(dailyRewardPort.findByUserAndDay(eq(42L), any())).thenReturn(Optional.empty());

        CheckinStatusView v = service.statusAt(42L, day, Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.dailyReward().streakDay()).isEqualTo(7);
        assertThat(v.dailyReward().todayBalanceCny()).isEqualByComparingTo("0.70");
        assertThat(v.dailyReward().todayNbPoints()).isEqualTo(21);
        assertThat(v.dailyReward().tomorrowStreakDay()).isEqualTo(8);
        assertThat(v.dailyReward().tomorrowBalanceCny()).isEqualByComparingTo("0.80");
        assertThat(v.dailyReward().tomorrowNbPoints()).isEqualTo(24);
        assertThat(v.dailyReward().balanceEligible()).isTrue();
        assertThat(v.dailyReward().balanceUnlockText()).isNull();
        assertThat(v.dailyReward().todayBalanceStatus()).isEqualTo("not_punched");
        assertThat(v.dailyReward().monthBalanceTotalCny()).isEqualByComparingTo("2.10");
    }

    @Test
    void ineligibleUserSeesLockedBalanceButStillGetsNb() {
        LocalDate day = LocalDate.of(2026, 8, 3);
        stubEligibleUserOnDate(day, 0, false);
        when(rechargePort.lifetimeRecharge(eq(42L), any())).thenReturn(new BigDecimal("29.99"));
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.myMonthlyBalanceTotal(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.findByUserAndDay(eq(42L), any())).thenReturn(Optional.empty());

        CheckinStatusView v = service.statusAt(42L, day, Instant.parse("2026-08-03T04:00:00Z"));

        assertThat(v.dailyReward().balanceEligible()).isFalse();
        assertThat(v.dailyReward().todayBalanceCny()).isEqualByComparingTo("0");
        assertThat(v.dailyReward().balanceUnlockText()).isEqualTo("累计充值满 ¥30 解锁返网费");
        assertThat(v.dailyReward().todayNbPoints()).isEqualTo(3);   // NB 无门槛,照给
    }

    @Test
    void tomorrowResetsToOneOnLastDayOfMonth() {
        // 8/31 已签、8/1~8/31 连签 31 天;明天是 9/1,自然月清零 → 第 1 天 ¥0.10
        LocalDate lastDay = LocalDate.of(2026, 8, 31);
        stubEligibleUserOnDate(lastDay, 31, true);
        when(rechargePort.lifetimeRecharge(eq(42L), any())).thenReturn(new BigDecimal("100"));
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.myMonthlyBalanceTotal(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.findByUserAndDay(eq(42L), any())).thenReturn(Optional.empty());

        CheckinStatusView v = service.statusAt(42L, lastDay, Instant.parse("2026-08-31T04:00:00Z"));

        assertThat(v.dailyReward().streakDay()).isEqualTo(31);
        assertThat(v.dailyReward().todayBalanceCny()).isEqualByComparingTo("3.10");
        assertThat(v.dailyReward().tomorrowStreakDay()).isEqualTo(1);
        assertThat(v.dailyReward().tomorrowBalanceCny()).isEqualByComparingTo("0.10");
    }

    @Test
    void tomorrowIsAlsoOneWhenTodayNotPunchedYet() {
        // 今天还没签 → 今天这条连签已经断了,明天签只能是本月第 1 天,
        // 绝不能画成 streakDay+1(那是用户拿不到的档位)。
        LocalDate day = LocalDate.of(2026, 8, 10);
        stubEligibleUserOnDate(day, 3, false);   // 8/7~8/9 签过,8/10 未签
        when(rechargePort.lifetimeRecharge(eq(42L), any())).thenReturn(new BigDecimal("100"));
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.myMonthlyBalanceTotal(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(dailyRewardPort.findByUserAndDay(eq(42L), any())).thenReturn(Optional.empty());

        CheckinStatusView v = service.statusAt(42L, day, Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.dailyReward().streakDay()).isEqualTo(4);         // 今天签的话是第 4 天
        assertThat(v.dailyReward().tomorrowStreakDay()).isEqualTo(1); // 但今天不签,明天从 1 起
    }

    /// stub 一个账龄合格的用户在指定基准日的当月签到史。
    ///
    /// @param includeToday true = 今天也已签(连签含今天);false = 今天还没签
    private void stubEligibleUserOnDate(LocalDate baseDate, int consecutiveDays, boolean includeToday) {
        when(registrationPort.registeredAt(42L))
                .thenReturn(Optional.of(Instant.parse("2020-01-01T00:00:00Z")));
        List<LocalDate> dates = new java.util.ArrayList<>();
        LocalDate cursor = includeToday ? baseDate : baseDate.minusDays(1);
        for (int i = 0; i < consecutiveDays; i++) {
            dates.add(cursor.minusDays(i));
        }
        when(checkinPort.datesInRange(eq(42L), any(), any())).thenReturn(dates);
        when(checkinPort.checkedInOn(42L, baseDate)).thenReturn(includeToday);
        when(checkinPort.totalCheckins(42L)).thenReturn(consecutiveDays);
    }

    @Test
    void tierEligibilityAchievedOnceCumulativeDaysReachThreshold() {
        // 2026-07-31 放宽:加时资格 = 当月累计签满 N 天,**不再要求一天不落、也不再等月末**。
        // 累计 20 天即达标,哪怕中间断过好几次。
        List<CheckinMilestoneView> milestones = CheckinStatusQueryService.buildMilestones(20, 20);

        CheckinMilestoneView fullMonth = milestones.get(3);
        assertThat(fullMonth.code()).isEqualTo("full_month");
        assertThat(fullMonth.label()).isEqualTo("加时资格");
        assertThat(fullMonth.target()).isEqualTo(20);
        assertThat(fullMonth.achieved()).isTrue();
        assertThat(fullMonth.statusText()).isEqualTo("已打穿");
    }

    @Test
    void tierEligibilityShowsProgressWhenCumulativeDaysStillShort() {
        // 月中累计 8 天:未达标,展示 "8 / 20" 的追赶进度——不再是旧口径那种
        // 「断一次就整月无望」的"本月已错过"死局。
        List<CheckinMilestoneView> milestones = CheckinStatusQueryService.buildMilestones(8, 20);

        CheckinMilestoneView fullMonth = milestones.get(3);
        assertThat(fullMonth.achieved()).isFalse();
        assertThat(fullMonth.statusText()).isEqualTo("8 / 20");
    }

    /// 门槛 2026-07-31 二次拍板 20→15 后,「加时资格」的 target 落在 5/10/20 三枚出勤徽章
    /// **中间**。若仍固定排第四,rail 上会读成 5 ✓ / 10 ✓ / 20「15 / 20」/ 加时资格「已打穿」
    /// ——后面那格先亮、前面那格还没到,自相矛盾。故按 target 升序排(稳定排序:同值时
    /// 出勤徽章在前),门槛怎么调都保持单调。
    @Test
    void milestonesStayInAscendingTargetOrderWhenThresholdFallsBetweenBadges() {
        List<CheckinMilestoneView> milestones = CheckinStatusQueryService.buildMilestones(16, 15);

        assertThat(milestones).extracting(CheckinMilestoneView::target)
                .containsExactly(5, 10, 15, 20);
        assertThat(milestones).extracting(CheckinMilestoneView::code)
                .containsExactly("days_5", "days_10", "full_month", "days_20");
        // 单调性的实质:达成态不能出现「后面亮着、前面没亮」
        assertThat(milestones).extracting(CheckinMilestoneView::achieved)
                .containsExactly(true, true, true, false);
    }

    /// 门槛与某枚徽章同值时,徽章排前(稳定排序)——避免顺序在等值边界上抖动。
    @Test
    void milestonesKeepBadgeBeforeQualificationOnTie() {
        List<CheckinMilestoneView> milestones = CheckinStatusQueryService.buildMilestones(3, 20);

        assertThat(milestones).extracting(CheckinMilestoneView::code)
                .containsExactly("days_5", "days_10", "days_20", "full_month");
    }

    @Test
    void gaugePctFollowsSegmentedScaleAcrossTierBoundaries() {
        // 分段刻度公式(2026-07-14 控制器裁决,替换 spec 草稿"朝下一档线性"公式):
        // 刻度 0/A/B/C 依次立于 0%/33%/66%/100%,段内线性,四舍五入取整;
        // 每段另封顶(2026-07-14 复审裁决),四舍五入不得把未达标金额显示成下一档整格刻度。
        assertThat(gaugePctFor(101, "0")).isEqualTo(0);
        assertThat(gaugePctFor(102, "30")).isEqualTo(33);    // 恰达 A(刻度本身,非封顶)
        assertThat(gaugePctFor(103, "36")).isEqualTo(43);    // 契约锚点
        assertThat(gaugePctFor(104, "49.8")).isEqualTo(65);  // 未达 B:round(65.67)=66 被封顶到 65
        assertThat(gaugePctFor(105, "50")).isEqualTo(66);    // 恰达 B(刻度本身,非封顶)
        assertThat(gaugePctFor(106, "275")).isEqualTo(83);   // B~C 中点
        assertThat(gaugePctFor(107, "495")).isEqualTo(99);   // 未达 C:round(99.62)=100 被封顶到 99
        assertThat(gaugePctFor(108, "500")).isEqualTo(100);  // 恰达 C(刻度本身,非封顶)
        assertThat(gaugePctFor(109, "600")).isEqualTo(100);  // 超 C
    }

    private int gaugePctFor(long userId, String monthlyRechargeCny) {
        when(registrationPort.registeredAt(userId)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        when(checkinPort.checkedInOn(eq(userId), any())).thenReturn(false);
        when(checkinPort.datesInRange(eq(userId), any(), any())).thenReturn(List.of());
        when(checkinPort.totalCheckins(userId)).thenReturn(0);
        when(checkinPort.countInRange(eq(userId), any(), any())).thenReturn(0);
        when(rechargePort.monthlyRecharge(eq(userId), any(), any())).thenReturn(new BigDecimal(monthlyRechargeCny));
        return service.status(userId).supply().gaugePct();
    }

    // ---- 准入闸(spec §12,2026-07-31):近 30 天真实充值 ≥¥30 才能上机 ----

    @Test
    void entryGateLockedWhenWindowRechargeShort() {
        LocalDate day = LocalDate.of(2026, 8, 10);
        stubEligibleUserOnDate(day, 0, false);
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(rechargePort.rechargeEvents(eq(42L), any(), any()))
                .thenReturn(List.of(new RechargeEvent(Instant.parse("2026-08-01T04:00:00Z"),
                        new BigDecimal("12"))));

        CheckinStatusView v = serviceWith(balanceProps, new CheckinEntryGateProperties(true, 30, "30"))
                .statusAt(42L, day, Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.eligible()).isFalse();
        assertThat(v.ineligibleReason()).isEqualTo("recharge_required");
        assertThat(v.entryGate().eligible()).isFalse();
        assertThat(v.entryGate().minCny()).isEqualByComparingTo("30");
        assertThat(v.entryGate().windowDays()).isEqualTo(30);
        assertThat(v.entryGate().rechargedCny()).isEqualByComparingTo("12");
        assertThat(v.entryGate().remainingDays()).isZero();
        assertThat(v.entryGate().noteText()).isEqualTo("近 30 天已充 ¥12 / 还差 ¥18");
    }

    @Test
    void entryGateOpenExposesRemainingDaysAsFinishedCopy() {
        // ¥30 充在 10 天前:窗口还罩得住 20 天——滚动窗口的「无预警断签」暗坑要变成明牌
        LocalDate day = LocalDate.of(2026, 8, 10);
        stubEligibleUserOnDate(day, 0, false);
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);
        when(rechargePort.rechargeEvents(eq(42L), any(), any()))
                .thenReturn(List.of(new RechargeEvent(Instant.parse("2026-07-31T04:00:00Z"),
                        new BigDecimal("30"))));

        CheckinStatusView v = serviceWith(balanceProps, new CheckinEntryGateProperties(true, 30, "30"))
                .statusAt(42L, day, Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.eligible()).isTrue();
        assertThat(v.ineligibleReason()).isNull();
        assertThat(v.entryGate().eligible()).isTrue();
        assertThat(v.entryGate().remainingDays()).isEqualTo(20);
        assertThat(v.entryGate().noteText()).isEqualTo("网费还够 20 天");
    }

    @Test
    void entryGateDisabledKeepsLegacyShape() {
        LocalDate day = LocalDate.of(2026, 8, 10);
        stubEligibleUserOnDate(day, 0, false);
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);

        CheckinStatusView v = service.statusAt(42L, day, Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.eligible()).isTrue();
        assertThat(v.entryGate()).isNull();
        verify(rechargePort, never()).rechargeEvents(anyLong(), any(), any());
    }

    @Test
    void accountTooNewShortCircuitsEntryGate() {
        // 账龄不够连闸都不开:reason 保持 account_too_new,不发逐笔充值 SQL
        when(registrationPort.registeredAt(7L)).thenReturn(Optional.empty());
        when(checkinPort.checkedInOn(eq(7L), any())).thenReturn(false);
        when(checkinPort.datesInRange(eq(7L), any(), any())).thenReturn(List.of());
        when(checkinPort.totalCheckins(7L)).thenReturn(0);
        when(rechargePort.monthlyRecharge(eq(7L), any(), any())).thenReturn(BigDecimal.ZERO);

        CheckinStatusView v = serviceWith(balanceProps, new CheckinEntryGateProperties(true, 30, "30"))
                .statusAt(7L, LocalDate.of(2026, 8, 10), Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.ineligibleReason()).isEqualTo("account_too_new");
        assertThat(v.entryGate()).isNull();
        verify(rechargePort, never()).rechargeEvents(anyLong(), any(), any());
    }

    @Test
    void steppedLadderPricesPairsAndExposesRateParams() {
        // 两天一档:连签第 3 天 ¥0.20,明天第 4 天还是 ¥0.20;费率参数随视图下发,
        // 前端画任意档位用 perDayCny × ⌈k/stepDays⌉,不得再用 today÷N 反推
        LocalDate day = LocalDate.of(2026, 8, 10);
        stubEligibleUserOnDate(day, 3, true);
        when(rechargePort.lifetimeRecharge(eq(42L), any())).thenReturn(new BigDecimal("100"));
        when(rechargePort.monthlyRecharge(eq(42L), any(), any())).thenReturn(BigDecimal.ZERO);

        CheckinStatusView v = serviceWith(new CheckinBalanceProperties(true, "0.1", "30", "3000", 2),
                new CheckinEntryGateProperties(false, 30, "30"))
                .statusAt(42L, day, Instant.parse("2026-08-10T04:00:00Z"));

        assertThat(v.dailyReward().streakDay()).isEqualTo(3);
        assertThat(v.dailyReward().todayBalanceCny()).isEqualByComparingTo("0.20");
        assertThat(v.dailyReward().tomorrowBalanceCny()).isEqualByComparingTo("0.20");
        assertThat(v.dailyReward().perDayCny()).isEqualByComparingTo("0.1");
        assertThat(v.dailyReward().stepDays()).isEqualTo(2);
    }
}
