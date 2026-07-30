package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.app.usecase.thursday.ThursdayGuessView;

/// 猜桶竞猜响应:九字段白名单。只含本人猜测与全场计数;
/// 赢家只回**他猜的数字**,不回是谁(payload 纪律:任何端点不吐他人身份)。
public record ThursdayGuessResponse(boolean eligible, boolean open, Integer myGuess, long count,
        Instant closeAt, BigDecimal thresholdCny, Integer answer, Integer winnerGuess, boolean iWon) {

    public static ThursdayGuessResponse of(ThursdayGuessView v) {
        return new ThursdayGuessResponse(v.eligible(), v.open(), v.myGuess(), v.count(), v.closeAt(),
                v.thresholdCny(), v.answer(), v.winnerGuess(), v.iWon());
    }
}
