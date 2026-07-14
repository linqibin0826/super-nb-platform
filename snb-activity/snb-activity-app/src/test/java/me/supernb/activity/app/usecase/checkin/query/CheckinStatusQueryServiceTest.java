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
import me.supernb.activity.domain.model.checkin.CheckinStatusView;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.junit.jupiter.api.Test;

/// 状态查询装配:字段形状按前端接线计划契约核对——eligible/ineligibleReason、
/// checkedDays 为"日"整数、里程碑成品文案、补给三档 state+statusText。
/// gaugePct 按 2026-07-14 控制器裁决采用分段刻度公式(刻度 0/A/B/C 立于 0%/33%/66%/100%,
/// 段内线性),覆盖 spec 草稿"朝下一档线性"公式——¥36 → 43 是与前端契约示例 JSON 对齐的锚点断言。
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
    void gaugePctFollowsSegmentedScaleAcrossTierBoundaries() {
        // 分段刻度公式(2026-07-14 控制器裁决,替换 spec 草稿"朝下一档线性"公式):
        // 刻度 0/A/B/C 依次立于 0%/33%/66%/100%,段内线性,四舍五入取整。
        assertThat(gaugePctFor(101, "0")).isEqualTo(0);
        assertThat(gaugePctFor(102, "30")).isEqualTo(33);   // 恰达 A
        assertThat(gaugePctFor(103, "36")).isEqualTo(43);   // 契约锚点
        assertThat(gaugePctFor(104, "50")).isEqualTo(66);   // 恰达 B
        assertThat(gaugePctFor(105, "275")).isEqualTo(83);  // B~C 中点
        assertThat(gaugePctFor(106, "500")).isEqualTo(100); // 恰达 C
        assertThat(gaugePctFor(107, "600")).isEqualTo(100); // 超 C
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
