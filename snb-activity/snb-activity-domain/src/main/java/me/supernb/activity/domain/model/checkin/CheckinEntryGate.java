package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/// 签到准入闸纯计算(2026-07-31 站长拍板,spec §12):近 windowDays 天真实充值 ≥ minCny
/// 才能上机——把签到从「白送」改成「付费客户回馈」,想一直签就得每 30 天续网费。
///
/// 窗口按 Asia/Shanghai 自然日闭区间 [today-(windowDays-1), today],共 windowDays 个自然日
/// (调用方负责把充值时刻换算成 CST 日期传入)。remainingDays 把滚动窗口的暗坑变成明牌:
/// 窗口逐日前滑、旧充值滑出后闸门会**无预警关闭**、连签随之断——提前把「网费还够几天」
/// 亮给用户,跟网吧包时卡一个逻辑。
public final class CheckinEntryGate {

    private CheckinEntryGate() {
    }

    /// 一笔真实充值(day 为到账时刻的 CST 自然日)。
    public record Event(LocalDate day, BigDecimal amount) {
    }

    /// 判定结果。
    ///
    /// @param eligible      窗口内合计是否 ≥ minCny
    /// @param rechargedCny  窗口内真实充值合计(锁态下页面拿它算「还差 ¥Y」)
    /// @param remainingDays 从今天(含)起不再充值、闸门还能连续保持开启的天数;锁态恒 0
    public record Result(boolean eligible, BigDecimal rechargedCny, int remainingDays) {
    }

    /// 评估准入闸。events 可比窗口略宽(防调用方查询边界不齐),窗口外一律忽略。
    public static Result evaluate(List<Event> events, LocalDate today, int windowDays, BigDecimal minCny) {
        BigDecimal recharged = sumSince(events, today.minusDays(windowDays - 1L), today);
        if (recharged.compareTo(minCny) < 0) {
            return new Result(false, recharged, 0);
        }
        // 窗口前滑只会让旧充值滑出、合计单调不增,从明天起逐日推进到首个跌破 minCny 的日子。
        // 上限 windowDays:今天刚充的钱也只养得起含今天在内的整窗。
        int remaining = 1;
        while (remaining < windowDays) {
            LocalDate slidWindowStart = today.plusDays(remaining).minusDays(windowDays - 1L);
            if (sumSince(events, slidWindowStart, today).compareTo(minCny) < 0) {
                break;
            }
            remaining++;
        }
        return new Result(true, recharged, remaining);
    }

    private static BigDecimal sumSince(List<Event> events, LocalDate fromInclusive, LocalDate toInclusive) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Event e : events) {
            if (!e.day().isBefore(fromInclusive) && !e.day().isAfter(toInclusive)) {
                sum = sum.add(e.amount());
            }
        }
        return sum;
    }
}
