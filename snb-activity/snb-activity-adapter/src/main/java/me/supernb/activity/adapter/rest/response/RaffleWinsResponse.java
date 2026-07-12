package me.supernb.activity.adapter.rest.response;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;

/// 公开中奖记录响应(按人聚合)。**没有 payload/userId 字段。**
public record RaffleWinsResponse(String displayName, List<Item> wins) {

    /// 组装。
    public static RaffleWinsResponse of(PersonWinsView v) {
        return new RaffleWinsResponse(v.displayName(), v.wins().stream()
                .map(w -> new Item(String.valueOf(w.campaignId()), w.campaignName(), w.drawnAt(),
                        w.tier(), w.prizeDisplayName()))
                .toList());
    }

    /// 单次中奖条目(雪花 id 对外一律字符串)。
    public record Item(String campaignId, String campaignName, Instant drawnAt, String tier,
            String prizeDisplayName) {}
}
