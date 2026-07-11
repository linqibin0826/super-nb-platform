package me.supernb.activity.domain.port.raffle;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.RaffleEntrant;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;

/// 报名端口。enter 的幂等与取号原子性由实现保证(campaign 级 advisory lock+唯一约束)。
public interface RaffleEntryPort {

    /// 报名:已报过 → 返回既有参会证(already=true,不新增);否则取号落库。
    RaffleEntryTicket enter(long campaignId, long userId, BigDecimal gateValue, String clientIp, String userAgent);

    /// 本人报名记录。
    Optional<RaffleEntrant> find(long campaignId, long userId);

    /// 该期报名人数。
    int count(long campaignId);

    /// 最近报名 limit 条(列席名单滚动)。
    List<RaffleEntrant> recent(long campaignId, int limit);

    /// 该期全量报名(开奖用)。
    List<RaffleEntrant> entrants(long campaignId);
}
