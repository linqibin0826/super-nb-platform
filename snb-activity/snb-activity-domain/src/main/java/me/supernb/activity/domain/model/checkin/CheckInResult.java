package me.supernb.activity.domain.model.checkin;

import java.time.LocalDate;

/// 签到成功后的对外契约结果(POST /checkin 200 响应体的领域来源)。
///
/// @param checkinDate    本次签到的自然日
/// @param cumulativeDays 历史累计签到天数(含本次)
/// @param streakCurrent  连续签到天数(纯展示,含本次)
public record CheckInResult(LocalDate checkinDate, int cumulativeDays, int streakCurrent) {
}
