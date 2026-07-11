package me.supernb.activity.app.usecase.usageboard;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;

/// 榜单周期边界计算(Asia/Shanghai 口径),供 UsageBoardCache 刷新时确定聚合窗口起点与展示用的结算时刻。
public final class BoardPeriods {

    /// 全站周期边界统一时区:上海(北京时间),不随服务器部署时区漂移。
    public static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private BoardPeriods() {
    }

    /// 周期起点(含):day=当日 00:00、week=本周一 00:00、month=当月 1 日 00:00、all=Instant.EPOCH。
    public static Instant start(BoardPeriod period, Instant now) {
        ZonedDateTime z = now.atZone(SHANGHAI);
        LocalDate today = z.toLocalDate();
        return switch (period) {
            case DAY -> today.atStartOfDay(SHANGHAI).toInstant();
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(SHANGHAI).toInstant();
            case MONTH -> today.withDayOfMonth(1).atStartOfDay(SHANGHAI).toInstant();
            case ALL -> Instant.EPOCH;
        };
    }

    /// 周期终点(排他,供前端结算倒计时展示):day=次日 00:00、week=下周一 00:00、month=次月 1 日 00:00、all=null(无边界)。
    public static Instant endsAt(BoardPeriod period, Instant now) {
        ZonedDateTime z = now.atZone(SHANGHAI);
        LocalDate today = z.toLocalDate();
        return switch (period) {
            case DAY -> today.plusDays(1).atStartOfDay(SHANGHAI).toInstant();
            case WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(1).atStartOfDay(SHANGHAI).toInstant();
            case MONTH -> today.withDayOfMonth(1).plusMonths(1).atStartOfDay(SHANGHAI).toInstant();
            case ALL -> null;
        };
    }
}
