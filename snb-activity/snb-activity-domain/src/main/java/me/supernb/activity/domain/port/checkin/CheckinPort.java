package me.supernb.activity.domain.port.checkin;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;

/// 签到聚合端口(活动库):写入与查询合一,照 RaffleEntryPort 惯例——同一聚合的读写共享一个端口。
public interface CheckinPort {

    /// 签到:当日已有记录 → 幂等回放(firstCheckinToday=false,checkedInAt 为原始时刻);
    /// 否则原子插入(INSERT ... ON CONFLICT DO NOTHING RETURNING id),并发天然去重,不抛异常)。
    CheckinOutcome checkIn(long userId, LocalDate day, Instant now);

    /// 是否已在某天签到。
    boolean checkedInOn(long userId, LocalDate day);

    /// [fromInclusive, toInclusive] 闭区间内的签到天数。
    int countInRange(long userId, LocalDate fromInclusive, LocalDate toInclusive);

    /// 全部历史签到天数(单用户至多一天一行,COUNT(*) 现查即可)。
    int totalCheckins(long userId);

    /// [fromInclusive, toInclusive] 闭区间内的签到日期,按日期降序。
    List<LocalDate> datesInRange(long userId, LocalDate fromInclusive, LocalDate toInclusive);

    /// [fromInclusive, toInclusive] 闭区间内每天都签到的用户 id 列表(满勤判定用,
    /// expectedDays = 该区间天数,调用方负责按上线日与自然月边界算好)。
    List<Long> fullAttendanceUserIds(LocalDate fromInclusive, LocalDate toInclusive, long expectedDays);
}
