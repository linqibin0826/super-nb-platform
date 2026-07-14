package me.supernb.activity.domain.model.checkin;

/// 单条里程碑状态(GET /checkin/status.milestones[] 的领域来源)。
///
/// @param code       稳定标识("days_5"/"days_10"/"days_20"/"full_month")
/// @param label      展示标签("出勤 5 天"等)
/// @param target     目标值(天数)
/// @param achieved   是否已达成
/// @param statusText 成品状态文案("已打穿"/"12 / 20"/"在轨 · 一格没漏")
public record CheckinMilestoneView(String code, String label, int target, boolean achieved, String statusText) {
}
