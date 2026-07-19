package me.supernb.activity.adapter.rest.response;

import java.time.Instant;
import me.supernb.activity.domain.model.raffle.RafflePrize;

/// 管理端奖品视图,⚠️ 与公开响应不同——payload 明文可见,仅供 admin 端点使用。
/// id/winnerUserId 在此转成 String(雪花 id 超出 JS 安全整数范围,前端按 string 消费——仓库里
/// 没找到全局 Jackson Long→String 序列化配置,DTO 层自己转,别赌默认行为)。
public record RaffleAdminPrize(String id, String tier, String displayName, String kind, String payload,
        int sortOrder, String winnerUserId, Instant assignedAt) {

    public static RaffleAdminPrize of(RafflePrize p) {
        return new RaffleAdminPrize(String.valueOf(p.id()), p.tier(), p.displayName(), p.kind(), p.payload(),
                p.sortOrder(), p.winnerUserId() == null ? null : String.valueOf(p.winnerUserId()), p.assignedAt());
    }
}
