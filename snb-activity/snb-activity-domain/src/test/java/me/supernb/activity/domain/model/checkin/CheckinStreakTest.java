package me.supernb.activity.domain.model.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/// streak(连续签到天数)纯展示,不作奖励判定依据(spec §3.1)——判定改用自然月累计天数,
/// 断签不清空月度累计;这里只验证"连续"这个纯粹的算法本身。
class CheckinStreakTest {

    @Test
    void noCheckinsYieldsZero() {
        assertThat(CheckinStreak.current(List.of(), LocalDate.of(2026, 7, 13))).isZero();
    }

    @Test
    void checkedInTodayCountsConsecutiveRunEndingToday() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 11));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 7, 13))).isEqualTo(3);
    }

    @Test
    void notYetCheckedInTodayStillCountsYesterdaysRun() {
        // 今天还没签到不该把昨天的连续记录清零——这是"断签保护"视觉呈现的算法基础
        List<LocalDate> dates = List.of(LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 11));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 7, 13))).isEqualTo(2);
    }

    @Test
    void gapBreaksStreakAtFirstMissingDay() {
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 12), LocalDate.of(2026, 7, 9));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 7, 13))).isEqualTo(2);
    }

    @Test
    void missedBothTodayAndYesterdayYieldsZero() {
        List<LocalDate> dates = List.of(LocalDate.of(2026, 7, 5));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 7, 13))).isZero();
    }

    // ---- 周末豁免(2026-08-17 站长拍板):没签的周六/周日跳过不计也不断,工作日缺口照断 ----

    @Test
    void uncheckedWeekendDoesNotBreakStreak() {
        // 周四 8/6、周五 8/7 签了,周六日(8/8、8/9)没签,周一 8/10 再签 → 3(周末冻结不清零)
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 6));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 8, 10))).isEqualTo(3);
    }

    @Test
    void checkedWeekendStillCounts() {
        // 周六 8/8 签了照常 +1、周日 8/9 没签跳过:8/7、8/8、8/10 → 3
        List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 8), LocalDate.of(2026, 8, 7));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 8, 10))).isEqualTo(3);
    }

    @Test
    void uncheckedWeekdayStillBreaks() {
        // 周五 8/7 没签(工作日缺口照断):周一 8/10 签 → 只有 1
        List<LocalDate> dates = List.of(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 6));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 8, 10))).isEqualTo(1);
    }

    @Test
    void weekendTodayNotYetCheckedKeepsWeekdayRun() {
        // 今天周日 8/16 还没签、周六 8/15 也没签 → 周四五(8/13、8/14)的连签不清零
        List<LocalDate> dates = List.of(LocalDate.of(2026, 8, 14), LocalDate.of(2026, 8, 13));
        assertThat(CheckinStreak.current(dates, LocalDate.of(2026, 8, 16))).isEqualTo(2);
    }
}
