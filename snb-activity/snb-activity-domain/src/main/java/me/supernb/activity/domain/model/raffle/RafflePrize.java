package me.supernb.activity.domain.model.raffle;

import java.time.Instant;

/// 奖品件(一件一行)。⚠️ payload 机密——本记录只准在开奖事务与「本人领奖」路径流转,
/// 组装公开读视图时不得透传 payload(公开视图类型上就没有该字段)。
public record RafflePrize(long id, String tier, String displayName, String kind, String payload,
        int sortOrder, Long winnerUserId, Instant assignedAt) {

    /// 是否已归属中奖者。
    public boolean assigned() {
        return winnerUserId != null;
    }
}
