package me.supernb.activity.domain.model.checkin;

import java.time.Instant;
import java.time.LocalDate;

/// 一次签到调用的结果:firstCheckinToday=false 表示幂等回放(当日已签到过,checkedInAt 是原始时刻)。
public record CheckinOutcome(boolean firstCheckinToday, LocalDate checkinDate, Instant checkedInAt) {
}
