package me.supernb.activity.domain.model.read.raffle;

import java.time.Instant;
import java.util.List;

/// 开奖结果公开视图(红头文件数据源):中奖名单脱敏,**无 payload 字段**。
public record RaffleResultView(long campaignId, String name, Instant drawnAt,
        int entrantCountAtDraw, int disqualifiedCount, List<Winner> winners) {

    /// 中奖行:参会证号+脱敏名+档位+奖品展示名。
    public record Winner(int entryNo, String displayName, String tier, String prizeDisplayName) {}
}
