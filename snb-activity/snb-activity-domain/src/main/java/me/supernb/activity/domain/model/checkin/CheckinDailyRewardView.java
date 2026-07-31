package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;

/// 连签阶梯读视图(spec 2026-07-31-checkin-daily-reward §4)。
/// 业务文案一律服务端算成成品字符串,前端不得用 `new Date()` 重推日期。
///
/// @param streakDay            今天的连签天数 N(今天未签时 =「若现在签到将得到的 N」)
/// @param todayBalanceCny      今天应得返网费;未达标 / 总闸关 / 预算硬顶打满时为 0
/// @param todayNbPoints        今天应得 NB(无门槛,人人有)
/// @param todayBalanceStatus   台账四态 none|pending|success|failed,外加 API 独有的
///                             `not_punched`(今天还没打卡、台账无行,**不入库**)
/// @param tomorrowStreakDay    明天的 N;今天没签或明天跨月一律为 1
/// @param tomorrowBalanceCny   明天应得返网费
/// @param tomorrowNbPoints     明天应得 NB
/// @param balanceEligible      是否已过历史累计充值门槛
/// @param balanceUnlockText    未达标时的成品解锁文案;已达标为 null
/// @param monthBalanceTotalCny 本月已返网费累计
public record CheckinDailyRewardView(int streakDay, BigDecimal todayBalanceCny, int todayNbPoints,
        String todayBalanceStatus, int tomorrowStreakDay, BigDecimal tomorrowBalanceCny,
        int tomorrowNbPoints, boolean balanceEligible, String balanceUnlockText,
        BigDecimal monthBalanceTotalCny) {
}
