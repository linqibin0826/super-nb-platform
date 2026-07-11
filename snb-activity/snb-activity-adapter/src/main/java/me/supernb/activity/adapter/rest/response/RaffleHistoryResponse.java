package me.supernb.activity.adapter.rest.response;

import java.time.Instant;
import me.supernb.activity.domain.model.read.raffle.RaffleHistoryItem;

/// 历届通报存档条目响应。
public record RaffleHistoryResponse(String id, String name, Instant drawnAt, int prizeCount, int entrantCount) {

    /// 组装。
    public static RaffleHistoryResponse of(RaffleHistoryItem i) {
        return new RaffleHistoryResponse(String.valueOf(i.id()), i.name(), i.drawnAt(),
                i.prizeCount(), i.entrantCount());
    }
}
