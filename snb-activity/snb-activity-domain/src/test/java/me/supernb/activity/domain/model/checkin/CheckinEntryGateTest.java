package me.supernb.activity.domain.model.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/// 签到准入闸纯计算:近 windowDays 天真实充值 ≥ minCny 才能上机(2026-07-31 站长拍板)。
/// 窗口按自然日闭区间 [today-(windowDays-1), today];剩余天数 = 从今天起不再充值、
/// 闸门还能连续保持开启的天数(窗口逐日前滑,旧充值滑出即失效)。
class CheckinEntryGateTest {

    private static final BigDecimal MIN = new BigDecimal("30");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Test
    void sumInWindowAtOrAboveMinOpensGate() {
        var r = CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(5), "30.00")), TODAY, 30, MIN);
        assertThat(r.eligible()).isTrue();
        assertThat(r.rechargedCny()).isEqualByComparingTo("30.00");
    }

    @Test
    void justBelowMinStaysLocked() {
        // ¥29.99 差一分也不行——锁态下 rechargedCny 照报,页面要拿它算「还差 ¥Y」
        var r = CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(1), "29.99")), TODAY, 30, MIN);
        assertThat(r.eligible()).isFalse();
        assertThat(r.rechargedCny()).isEqualByComparingTo("29.99");
        assertThat(r.remainingDays()).isZero();
    }

    @Test
    void eventExactlyAtWindowEdgeCounts() {
        // 30 天窗含今天共 30 个自然日:today-29 在窗内,today-30 已滑出
        var inEdge = CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(29), "30.00")), TODAY, 30, MIN);
        assertThat(inEdge.eligible()).isTrue();

        var outEdge = CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(30), "30.00")), TODAY, 30, MIN);
        assertThat(outEdge.eligible()).isFalse();
        assertThat(outEdge.rechargedCny()).isEqualByComparingTo("0");
    }

    @Test
    void remainingDaysCountsFromTodayInclusive() {
        // 今天刚充 ¥30 → 含今天整整 30 天;充值在 10 天前 → 只剩 20 天
        assertThat(CheckinEntryGate.evaluate(
                List.of(event(TODAY, "30.00")), TODAY, 30, MIN).remainingDays()).isEqualTo(30);
        assertThat(CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(10), "30.00")), TODAY, 30, MIN).remainingDays()).isEqualTo(20);
        assertThat(CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(29), "30.00")), TODAY, 30, MIN).remainingDays()).isEqualTo(1);
    }

    @Test
    void remainingDaysEndsWhenOlderSliceFallsOut() {
        // 拆单 ¥15+¥15(15 天前 + 今天):老的那笔 15 天后滑出,合计跌破 ¥30 → 剩 15 天,
        // 不能按最新一笔算成 30 天
        var r = CheckinEntryGate.evaluate(
                List.of(event(TODAY.minusDays(15), "15.00"), event(TODAY, "15.00")), TODAY, 30, MIN);
        assertThat(r.eligible()).isTrue();
        assertThat(r.remainingDays()).isEqualTo(15);
    }

    @Test
    void noEventsMeansLockedWithZeroes() {
        var r = CheckinEntryGate.evaluate(List.of(), TODAY, 30, MIN);
        assertThat(r.eligible()).isFalse();
        assertThat(r.rechargedCny()).isEqualByComparingTo("0");
        assertThat(r.remainingDays()).isZero();
    }

    private static CheckinEntryGate.Event event(LocalDate day, String amount) {
        return new CheckinEntryGate.Event(day, new BigDecimal(amount));
    }
}
