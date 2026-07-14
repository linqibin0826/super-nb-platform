package me.supernb.activity.app.usecase.checkin.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinTierProperties;
import me.supernb.activity.domain.model.checkin.CheckinMilestoneView;
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
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
    private final CheckinProperties props = new CheckinProperties("2020-01-01");
    private final CheckinTierProperties tierProps = new CheckinTierProperties(
            new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("500"));
    private final CheckinStatusQueryService service =
            new CheckinStatusQueryService(checkinPort, rechargePort, registrationPort, props, tierProps);

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

    @Test
    void fullMonthAchievedOnlyOnLastDayOfMonthEvenWhenLaunchDateFallsMidMonth() {
        // 上线月结构性可达性修复(2026-07-14 复审裁决):launchDate 落在被测月中旬时,
        // monthCount(整月签到数)天然小于 monthDays(全月天数),不能再用两者比较判定满勤。
        // 7/13 上线,用户从上线日起一天不落到月末(7/13~7/31 共 19 天)——今天已是月末,应判定满勤。
        List<CheckinMilestoneView> milestones =
                CheckinStatusQueryService.buildMilestones(19, true, LocalDate.of(2026, 7, 31));

        CheckinMilestoneView fullMonth = milestones.get(3);
        assertThat(fullMonth.code()).isEqualTo("full_month");
        assertThat(fullMonth.target()).isEqualTo(31);
        assertThat(fullMonth.achieved()).isTrue();
        assertThat(fullMonth.statusText()).isEqualTo("已打穿");
    }

    @Test
    void fullMonthNotYetAchievedMidMonthEvenWhenOnTrackSinceLaunch() {
        // 同样从上线日(7/13)起一天不落,但今天只是 7/20(月中,非月末,7/13~7/20 共 8 天)——
        // 不该提前判定满勤,应保持"在轨"展示态,等到月末那天才翻转成"已打穿"。
        List<CheckinMilestoneView> milestones =
                CheckinStatusQueryService.buildMilestones(8, true, LocalDate.of(2026, 7, 20));

        CheckinMilestoneView fullMonth = milestones.get(3);
        assertThat(fullMonth.achieved()).isFalse();
        assertThat(fullMonth.statusText()).isEqualTo("在轨 · 一格没漏");
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
}
