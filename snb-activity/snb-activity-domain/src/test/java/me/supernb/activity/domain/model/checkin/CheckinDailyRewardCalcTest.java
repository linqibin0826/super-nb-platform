package me.supernb.activity.domain.model.checkin;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/// 连签第 N 天的日奖励纯计算:N 从月初起算(不越月)、断签归零、今天未签也按「若现在签到」给出 N。
class CheckinDailyRewardCalcTest {

    @Test
    void firstDayOfMonthIsAlwaysOne() {
        // 上月连签一整月,8/1 仍从 1 起——自然月清零
        LocalDate today = LocalDate.of(2026, 8, 1);
        assertThat(CheckinDailyRewardCalc.streakDay(List.of(), today)).isEqualTo(1);
    }

    @Test
    void continuesFromYesterdayWhenNotPunchedToday() {
        // 8/1~8/3 已签,今天 8/4 还没签 → 若现在签就是第 4 天
        LocalDate today = LocalDate.of(2026, 8, 4);
        List<LocalDate> month =
                List.of(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1));
        assertThat(CheckinDailyRewardCalc.streakDay(month, today)).isEqualTo(4);
    }

    @Test
    void sameValueWhenAlreadyPunchedToday() {
        // 同一天已签,N 不能变——status 查询与打卡写入两条路径必须同值
        LocalDate today = LocalDate.of(2026, 8, 4);
        List<LocalDate> month = List.of(LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1));
        assertThat(CheckinDailyRewardCalc.streakDay(month, today)).isEqualTo(4);
    }

    @Test
    void gapResetsToOne() {
        // 8/1、8/2 签了,8/3 漏了,8/4 再签 → 回到第 1 天
        LocalDate today = LocalDate.of(2026, 8, 4);
        List<LocalDate> month = List.of(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1));
        assertThat(CheckinDailyRewardCalc.streakDay(month, today)).isEqualTo(1);
    }

    @Test
    void doesNotCountAcrossMonthBoundary() {
        // 调用方只传本月日期,上月即使连签一整月也不参与——分母天然不越月
        LocalDate today = LocalDate.of(2026, 8, 1);
        assertThat(CheckinDailyRewardCalc.streakDay(List.of(), today)).isEqualTo(1);
    }

    @Test
    void balanceAndNbScaleWithStreakDay() {
        // stepDays=1 即原线性费率,老口径一分不变
        assertThat(CheckinDailyRewardCalc.nbPoints(7, 3)).isEqualTo(21);
        assertThat(CheckinDailyRewardCalc.balanceCny(7, new BigDecimal("0.1"), 1)).isEqualByComparingTo("0.70");
        assertThat(CheckinDailyRewardCalc.balanceCny(30, new BigDecimal("0.1"), 1)).isEqualByComparingTo("3.00");
        assertThat(CheckinDailyRewardCalc.balanceCny(31, new BigDecimal("0.1"), 1)).isEqualByComparingTo("3.10");
    }

    @Test
    void balanceStepsUpEveryTwoDays() {
        // 两天一档(2026-07-31 站长拍板):第 N 天返 perDay × ⌈N/2⌉——0.1/0.1/0.2/0.2/0.3…
        // 满月上限从 ¥49.6 压到 ¥25.6,低于准入闸的 ¥30 月充下限,铁杆用户不再倒挂
        BigDecimal perDay = new BigDecimal("0.1");
        assertThat(CheckinDailyRewardCalc.balanceCny(1, perDay, 2)).isEqualByComparingTo("0.10");
        assertThat(CheckinDailyRewardCalc.balanceCny(2, perDay, 2)).isEqualByComparingTo("0.10");
        assertThat(CheckinDailyRewardCalc.balanceCny(3, perDay, 2)).isEqualByComparingTo("0.20");
        assertThat(CheckinDailyRewardCalc.balanceCny(4, perDay, 2)).isEqualByComparingTo("0.20");
        assertThat(CheckinDailyRewardCalc.balanceCny(15, perDay, 2)).isEqualByComparingTo("0.80");
        assertThat(CheckinDailyRewardCalc.balanceCny(30, perDay, 2)).isEqualByComparingTo("1.50");
        assertThat(CheckinDailyRewardCalc.balanceCny(31, perDay, 2)).isEqualByComparingTo("1.60");
    }
}
