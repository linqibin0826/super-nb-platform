package me.supernb.activity.infra.adapter.persistence;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.activity.infra.adapter.persistence.dao.RafflePrizeJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RafflePrizeEntity;
import org.springframework.stereotype.Repository;

/// RafflePrizePort 实现:纯查询映射。payload 随记录流出,消费侧红线见端口注释。
@Repository
public class RafflePrizeAdapter implements RafflePrizePort {

    private final RafflePrizeJpaRepository prizes;

    /// 构造:注入奖品仓储。
    public RafflePrizeAdapter(RafflePrizeJpaRepository prizes) {
        this.prizes = prizes;
    }

    @Override
    public List<RafflePrize> byCampaign(long campaignId) {
        return prizes.findByCampaignIdOrderBySortOrderAscIdAsc(campaignId).stream()
                .map(RafflePrizeAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<RafflePrize> wonBy(long campaignId, long userId) {
        return prizes.findFirstByCampaignIdAndWinnerUserId(campaignId, userId)
                .map(RafflePrizeAdapter::toDomain);
    }

    /// 实体 -> 领域记录。
    static RafflePrize toDomain(RafflePrizeEntity e) {
        return new RafflePrize(e.getId(), e.getTier(), e.getDisplayName(), e.getKind(), e.getPayload(),
                e.getSortOrder(), e.getWinnerUserId(), e.getAssignedAt());
    }
}
