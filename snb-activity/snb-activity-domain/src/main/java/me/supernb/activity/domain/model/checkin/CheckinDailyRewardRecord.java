package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;
import java.time.LocalDate;

/// 单行每日返网费台账(领域读模型)。
///
/// @param id            台账行 id(雪花)
/// @param userId        用户 id
/// @param checkinDate   所属自然日
/// @param streakDay     当日连签第几天(N)
/// @param nbPoints      实发 NB
/// @param balanceCny    实发返网费;0 = 未达标/总闸关/预算硬顶打满
/// @param balanceStatus none | pending | success | failed
/// @param attempts      已尝试发放次数
public record CheckinDailyRewardRecord(long id, long userId, LocalDate checkinDate, int streakDay,
        int nbPoints, BigDecimal balanceCny, String balanceStatus, int attempts) {
}
