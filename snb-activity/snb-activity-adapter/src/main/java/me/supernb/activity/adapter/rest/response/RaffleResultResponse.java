package me.supernb.activity.adapter.rest.response;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.raffle.RaffleResultView;

/// 开奖结果公开响应(红头文件数据源)。**没有 payload 字段。**
public record RaffleResultResponse(String campaignId, String name, Instant drawnAt,
        int entrantCountAtDraw, int disqualifiedCount, List<Winner> winners) {

    /// 组装。
    public static RaffleResultResponse of(RaffleResultView v) {
        return new RaffleResultResponse(String.valueOf(v.campaignId()), v.name(), v.drawnAt(),
                v.entrantCountAtDraw(), v.disqualifiedCount(),
                v.winners().stream().map(w -> new Winner(w.entryNo(), w.displayName(),
                        w.tier(), w.prizeDisplayName())).toList());
    }

    /// 中奖行。
    public record Winner(int entryNo, String displayName, String tier, String prizeDisplayName) {}
}
