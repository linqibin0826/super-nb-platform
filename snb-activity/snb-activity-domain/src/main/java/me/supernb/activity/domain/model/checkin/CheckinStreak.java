package me.supernb.activity.domain.model.checkin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// 连续签到天数(streak)纯计算,无框架依赖。streak 只作展示,不影响任何奖励判定(spec §3.1/§3.2)。
public final class CheckinStreak {

    private CheckinStreak() {
    }

    /// 从 today 开始向前逐日回溯,已签的日期计数 +1,遇到没签的**工作日**即停止。
    /// 两条宽容(都是跳过不计、不断):today 当天尚未签到不因"今天还没点"就把昨天的连续记录清零;
    /// 没签的周六/周日冻结不清零(2026-08-17 站长拍板,周末签了则照常 +1)。
    ///
    /// @param recentDatesDesc 调用方已查好的近期签到日期集合(顺序不敏感,内部用 Set 判存在性)
    /// @param today           计算基准日(调用方按 Asia/Shanghai 换算好传入)
    public static int current(List<LocalDate> recentDatesDesc, LocalDate today) {
        Set<LocalDate> checkedDays = new HashSet<>(recentDatesDesc);
        int streak = 0;
        LocalDate cursor = today;
        while (checkedDays.contains(cursor) || cursor.equals(today) || isWeekend(cursor)) {
            if (checkedDays.contains(cursor)) {
                streak++;
            }
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private static boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }
}
