package me.supernb.activity.app.usecase.thursday;

import java.math.BigDecimal;
import java.time.Instant;

/// 猜桶竞猜视图。payload 纪律同疯四桶:只回本人的猜测与全场计数,
/// 不含任何他人身份——赢家也只回**他猜的数字**,不回是谁。
///
/// @param eligible    本人是否够门槛(累计真实充值 ≥ thresholdCny)
/// @param open        是否还能猜(场次日 且 未到封猜时刻)
/// @param myGuess     本人猜的份数;没猜过为 null
/// @param count       本场已猜人数
/// @param closeAt     封猜时刻
/// @param thresholdCny 参与门槛(元),前端照实显示,不写死
/// @param answer      结算后的实际出桶数;未结算为 null
/// @param winnerGuess 结算后赢家猜的数字;无人参与为 null
/// @param iWon        本人是否就是赢家
/// @param closeAtLabel  封猜时刻的成品文案(如 "20:00")
/// @param revealAtLabel 结算时刻的成品文案(如 "20:30")
///
/// 🚨 两个 label 必须由服务端给,前端**绝不许写死时刻**:这两个值是配置项
/// (THURSDAY_GUESS_CLOSE_AT / THURSDAY_REVEAL_AT),一改就会出现「页面写 22:00、
/// 实际按 20:30 判」——用户按看到的题面下注、系统按另一个时刻结算,那是真的改判。
/// 2026-07-30 一天之内这个时刻改了两次(22:00→20:30→21:00),页面上写死过一处都得追着改;
/// 靠这两个 label,改 compose 里的值 + 重建容器就够了,前端一行不用动。
///
/// ⚠️ 但要清楚:开盘后改这个时刻本身**就是**改判——答案窗口是 [当天 00:00, revealAt),
/// 往后推只会让出桶数变大,系统性地帮猜高的、坑猜低的。摇号与结算做成纯函数不落表,
/// 挡得住「重算出不同结果」,挡不住「换一个 revealAt」。所以开盘后再动它必须群里公示,
/// 别让人以为规则是偷偷变的。
public record ThursdayGuessView(boolean eligible, boolean open, Integer myGuess, long count,
        Instant closeAt, BigDecimal thresholdCny, Integer answer, Integer winnerGuess, boolean iWon,
        String closeAtLabel, String revealAtLabel) {

    /// 非场次日:整块不渲染。
    public static ThursdayGuessView closed(BigDecimal thresholdCny) {
        return new ThursdayGuessView(false, false, null, 0, null, thresholdCny, null, null, false,
                null, null);
    }
}
