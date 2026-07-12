package me.supernb.activity.domain.model.raffle;

import java.math.BigDecimal;
import java.time.Instant;

/// 发布会一期(报名开奖式抽奖)。字段与 `activity.raffle_campaign` 一一对应;
/// drawnAt/entrantCountAtDraw/disqualifiedCount 在开奖前为 null。
public record RaffleCampaign(long id, String name, Instant entryOpenAt, Instant entryCloseAt,
        Instant drawAt, GateType gateType, BigDecimal gateAmount, Instant gateFrom,
        Integer minAccountAgeDays, WeightMode weightMode, String status,
        Instant drawnAt, Integer entrantCountAtDraw, Integer disqualifiedCount) {

    /// 报名窗口判定:active 且 now ∈ [entryOpenAt, entryCloseAt)。
    public boolean openForEntry(Instant now) {
        return "active".equals(status) && !now.isBefore(entryOpenAt) && now.isBefore(entryCloseAt);
    }

    /// 已开奖判定。
    public boolean drawn() {
        return "drawn".equals(status);
    }
}
