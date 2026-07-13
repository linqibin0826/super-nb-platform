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
}
