package me.supernb.activity.app.usecase.gate;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.domain.model.gate.GateDrawOutcome;

/// 金票闸机抽签结果(对 adapter 的应用层视图):
/// eligible=false 表示门槛外或闸机休眠——前端与普通过闸零差异(完全隐身)。
public record GateDrawResult(boolean eligible, boolean win, BigDecimal amount, String code, Instant drawnAt) {

    /// 门槛外 / 休眠:一律不可见。
    public static GateDrawResult ineligible() {
        return new GateDrawResult(false, false, null, null, null);
    }

    /// 够格用户的抽签结果。
    public static GateDrawResult of(GateDrawOutcome o) {
        return new GateDrawResult(true, o.win(), o.amount(), o.code(), o.drawnAt());
    }
}
