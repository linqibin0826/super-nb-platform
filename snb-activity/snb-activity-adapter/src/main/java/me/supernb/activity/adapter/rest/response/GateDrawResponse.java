package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.app.usecase.gate.GateDrawResult;

/// 金票闸机响应:五字段白名单;code 只属于中签者本人(spec gate §4.3,payload 纪律)。
/// eligible=false(门槛外/休眠)时其余字段全空——前端与普通过闸零差异。
public record GateDrawResponse(boolean eligible, boolean win, BigDecimal amount, String code, Instant drawnAt) {

    public static GateDrawResponse of(GateDrawResult r) {
        return new GateDrawResponse(r.eligible(), r.win(), r.amount(), r.code(), r.drawnAt());
    }
}
