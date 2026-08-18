package me.supernb.activity.domain.model.raffle;

import java.math.BigDecimal;
import java.time.Instant;

/// 发布会一期(报名开奖式抽奖)。字段与 `activity.raffle_campaign` 一一对应;
/// drawnAt/entrantCountAtDraw/disqualifiedCount 在开奖前为 null。
/// minBalance=余额闸(2026-08-18):非 null 时「站内余额 ≥ minBalance」与充值闸取或,任一满足即有资格。
public record RaffleCampaign(long id, String name, Instant entryOpenAt, Instant entryCloseAt,
        Instant drawAt, GateType gateType, BigDecimal gateAmount, Instant gateFrom,
        BigDecimal minBalance, Integer minAccountAgeDays, WeightMode weightMode, String status,
        Instant drawnAt, Integer entrantCountAtDraw, Integer disqualifiedCount) {

    /// 兼容构造:未配置余额闸的调用点(admin 建期/存量测试)沿旧签名,minBalance=null。
    public RaffleCampaign(long id, String name, Instant entryOpenAt, Instant entryCloseAt,
            Instant drawAt, GateType gateType, BigDecimal gateAmount, Instant gateFrom,
            Integer minAccountAgeDays, WeightMode weightMode, String status,
            Instant drawnAt, Integer entrantCountAtDraw, Integer disqualifiedCount) {
        this(id, name, entryOpenAt, entryCloseAt, drawAt, gateType, gateAmount, gateFrom,
                null, minAccountAgeDays, weightMode, status, drawnAt, entrantCountAtDraw, disqualifiedCount);
    }

    /// 报名窗口判定:active 且 now ∈ [entryOpenAt, entryCloseAt)。
    public boolean openForEntry(Instant now) {
        return "active".equals(status) && !now.isBefore(entryOpenAt) && now.isBefore(entryCloseAt);
    }

    /// 已开奖判定。
    public boolean drawn() {
        return "drawn".equals(status);
    }
}
