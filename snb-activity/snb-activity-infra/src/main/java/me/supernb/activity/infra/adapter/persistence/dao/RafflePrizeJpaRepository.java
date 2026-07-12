package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.RafflePrizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// `activity.raffle_prize` 仓储。
public interface RafflePrizeJpaRepository extends JpaRepository<RafflePrizeEntity, Long> {

    /// 该期全部奖品件,按张榜顺序(sort_order,同序按 id)——议程单聚合与开奖分配共用。
    List<RafflePrizeEntity> findByCampaignIdOrderBySortOrderAscIdAsc(long campaignId);

    /// 本人在该期中的奖品件(一人至多一件,由开奖算法保证)。
    Optional<RafflePrizeEntity> findFirstByCampaignIdAndWinnerUserId(long campaignId, long userId);

    /// 某用户在所有已开奖期的历次中奖(只投影公开列,payload 不出库),按开奖时刻倒序。
    @Query("SELECT p.campaignId, c.name, c.drawnAt, p.tier, p.displayName "
            + "FROM RafflePrizeEntity p, RaffleCampaignEntity c "
            + "WHERE c.id = p.campaignId AND p.winnerUserId = :uid AND c.status = 'drawn' "
            + "ORDER BY c.drawnAt DESC, p.sortOrder ASC")
    List<Object[]> findWinsOf(@Param("uid") long userId);
}
