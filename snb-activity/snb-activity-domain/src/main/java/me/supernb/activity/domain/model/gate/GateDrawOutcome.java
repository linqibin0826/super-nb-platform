package me.supernb.activity.domain.model.gate;

import java.math.BigDecimal;
import java.time.Instant;

/// 金票闸机一次抽签的结果(读视图):未中时后三者为 null。
/// code 属于中签者本人,绝不进入任何公开聚合(payload 纪律,spec gate §4.3)。
public record GateDrawOutcome(boolean win, BigDecimal amount, String code, Instant drawnAt) {

    /// 未中(含池空、当日已抽未中的回放)。
    public static GateDrawOutcome lose() {
        return new GateDrawOutcome(false, null, null, null);
    }
}
