package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;

/// 准入闸读视图(spec §12)。闸门未启用时整块为 null(前端据此走旧行为)。
///
/// @param eligible      窗口内真实充值是否 ≥ minCny
/// @param minCny        门槛金额(元)
/// @param windowDays    滚动窗口天数(含今天)
/// @param rechargedCny  窗口内真实充值合计(锁态下前端拿它画「已充/还差」)
/// @param remainingDays 从今天(含)起不再充值、闸门还能保持开启的天数;锁态恒 0
/// @param noteText      成品提示文案(锁态「近 30 天已充 ¥X / 还差 ¥Y」;
///                      开启态「网费还够 N 天」,前端不得自行拼日期算术)
public record CheckinEntryGateView(boolean eligible, BigDecimal minCny, int windowDays,
        BigDecimal rechargedCny, int remainingDays, String noteText) {
}
