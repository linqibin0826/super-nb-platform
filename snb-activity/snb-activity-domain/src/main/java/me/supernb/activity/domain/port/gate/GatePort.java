package me.supernb.activity.domain.port.gate;

import java.time.LocalDate;
import me.supernb.activity.domain.exception.GateAlreadyAttemptedTodayException;
import me.supernb.activity.domain.model.gate.GateDrawOutcome;

/// 金票闸机事务端口(活动库)。实现须保证原子性:当日 attempt 唯一键仲裁 →
/// (wantWin 且池非空时)SKIP LOCKED 领码 → 落 attempt 记录。
public interface GatePort {

    /// 为用户执行/回放当日抽签:
    /// - 当日已有 attempt → 直接回放既有结果(中过则返回同一张码,防丢码);
    /// - 首次且 wantWin → 原子领取一张库存码(池空按未中落库);
    /// - 并发双击撞唯一键 → 抛 [GateAlreadyAttemptedTodayException](本事务回滚,领码不漏池),
    ///   由调用方以 wantWin=false 换新事务降级重读。
    GateDrawOutcome drawFor(long userId, LocalDate day, boolean wantWin);
}
