package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.RafflePrizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// `activity.raffle_prize` 仓储。
public interface RafflePrizeJpaRepository extends JpaRepository<RafflePrizeEntity, Long> {

    /// 该期全部奖品件,按张榜顺序(sort_order,同序按 id)——议程单聚合与开奖分配共用。
    List<RafflePrizeEntity> findByCampaignIdOrderBySortOrderAscIdAsc(long campaignId);

    /// 本人在该期中的奖品件(一人至多一件,由开奖算法保证)。
    Optional<RafflePrizeEntity> findFirstByCampaignIdAndWinnerUserId(long campaignId, long userId);
}
