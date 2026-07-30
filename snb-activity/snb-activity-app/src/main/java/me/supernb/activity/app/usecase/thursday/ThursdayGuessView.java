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
public record ThursdayGuessView(boolean eligible, boolean open, Integer myGuess, long count,
        Instant closeAt, BigDecimal thresholdCny, Integer answer, Integer winnerGuess, boolean iWon) {

    /// 非场次日:整块不渲染。
    public static ThursdayGuessView closed(BigDecimal thresholdCny) {
        return new ThursdayGuessView(false, false, null, 0, null, thresholdCny, null, null, false);
    }
}
