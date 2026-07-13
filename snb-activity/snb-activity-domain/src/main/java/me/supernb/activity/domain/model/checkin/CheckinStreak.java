package me.supernb.activity.domain.model.checkin;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// 连续签到天数(streak)纯计算,无框架依赖。streak 只作展示,不影响任何奖励判定(spec §3.1/§3.2)。
public final class CheckinStreak {

    private CheckinStreak() {
    }

    /// 从 today 开始向前逐日回溯,只要该日期在 recentDatesDesc 中存在就计数 +1,遇到第一个缺口即停止。
    /// today 当天尚未签到时从 today-1 开始计,不因"今天还没点"就把昨天的连续记录清零。
    ///
    /// @param recentDatesDesc 调用方已查好的近期签到日期集合(顺序不敏感,内部用 Set 判存在性)
    /// @param today           计算基准日(调用方按 Asia/Shanghai 换算好传入)
    public static int current(List<LocalDate> recentDatesDesc, LocalDate today) {
        Set<LocalDate> checkedDays = new HashSet<>(recentDatesDesc);
        LocalDate cursor = checkedDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (checkedDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
