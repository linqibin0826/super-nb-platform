package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/// 连签第 N 天的日奖励纯计算(spec 2026-07-31-checkin-daily-reward §1.1)。
///
/// N = 1 + (从昨天起往回逐日回溯,落在本月内且有签到记录的连续天数)。实现上把 today
/// 并入集合后复用 [CheckinStreak#current]——「今天已签」与「今天未签」两条路径由此天然同值,
/// status 查询与打卡写入可共用同一个 N,不会出现两处算出不同档位的裂缝。
/// 月初清零是白送的:调用方只传本月日期,回溯到月初自然停,无需额外边界判断。
public final class CheckinDailyRewardCalc {

    /// 金额标度,对齐 `checkin_daily_reward.balance_cny` 的 NUMERIC(10,2)。
    private static final int BALANCE_SCALE = 2;

    private CheckinDailyRewardCalc() {
    }

    /// 今天的连签天数 N(今天未签时给出「若现在签到将得到的 N」)。
    ///
    /// @param monthDates 本月已签到日期(顺序不敏感;调用方负责只传本自然月的)
    /// @param today      基准日(Asia/Shanghai 换算好传入)
    public static int streakDay(List<LocalDate> monthDates, LocalDate today) {
        List<LocalDate> withToday = new ArrayList<>(monthDates);
        withToday.add(today);
        return CheckinStreak.current(withToday, today);
    }

    /// 第 N 天的 NB 进账 = perDay × N。
    public static int nbPoints(int streakDay, int perDay) {
        return perDay * streakDay;
    }

    /// 第 N 天的返网费 = perDayCny × ⌈N/stepDays⌉,scale=2。
    ///
    /// stepDays=1 即原线性费率;stepDays=2 为「两天一档」(2026-07-31 站长拍板,spec §12):
    /// 0.1/0.1/0.2/0.2/0.3…,满月上限 ¥25.6 压到准入闸的 ¥30 月充下限之下,铁杆用户不再倒挂。
    public static BigDecimal balanceCny(int streakDay, BigDecimal perDayCny, int stepDays) {
        int tier = (streakDay + stepDays - 1) / stepDays;
        return perDayCny.multiply(BigDecimal.valueOf(tier)).setScale(BALANCE_SCALE, RoundingMode.HALF_UP);
    }
}
