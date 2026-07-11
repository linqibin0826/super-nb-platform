package me.supernb.activity.domain.model.raffle;

/// 开奖结果摘要(任务日志用):executed=false 表示 CAS 未命中(已被开过/非 active),本次为无操作。
public record RaffleDrawSummary(long campaignId, boolean executed, int winners, int disqualified) {

    /// CAS 未命中的无操作摘要。
    public static RaffleDrawSummary skipped(long campaignId) {
        return new RaffleDrawSummary(campaignId, false, 0, 0);
    }
}
