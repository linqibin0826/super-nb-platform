package me.supernb.activity.domain.model.checkin;

import java.util.List;

/// 签到状态视图(GET /checkin/status 的领域读模型,字段与前端接线计划契约总览逐一对应)。
///
/// @param eligible         是否可打卡(账龄门槛判定结果)
/// @param ineligibleReason 不可打卡原因("account_too_new" | null)
/// @param punchedToday     今日是否已打卡
/// @param todayDay         今天是本月第几天(服务端权威计算,前端不得用 Date 重推)
/// @param monthLabel       月份标签,如 "2026.07"
/// @param monthDays        本月总天数
/// @param checkedDays      本自然月已签到的"日"整数列表(如 [1,2,3])
/// @param cumulativeDays   历史累计签到天数
/// @param streakCurrent    连续签到天数(纯展示)
/// @param milestones       四档里程碑状态(5/10/20/满勤,固定顺序)
/// @param supply           补给资格进度总视图
public record CheckinStatusView(
        boolean eligible,
        String ineligibleReason,
        boolean punchedToday,
        int todayDay,
        String monthLabel,
        int monthDays,
        List<Integer> checkedDays,
        int cumulativeDays,
        int streakCurrent,
        List<CheckinMilestoneView> milestones,
        CheckinSupplyView supply) {
}
