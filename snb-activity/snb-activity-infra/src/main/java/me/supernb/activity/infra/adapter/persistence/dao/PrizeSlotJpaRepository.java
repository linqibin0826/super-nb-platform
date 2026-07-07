package me.supernb.activity.infra.adapter.persistence.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.PrizeSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// activity.prize_slot 仓储。
public interface PrizeSlotJpaRepository extends JpaRepository<PrizeSlotEntity, Long> {

    /// 随机锁定一个可用槽:FOR UPDATE SKIP LOCKED 为 PG 特有语义,保留 native;
    /// 返回的实体受管,置领取字段后随事务提交落库。
    @Query(value = "SELECT * FROM activity.prize_slot WHERE campaign_id = :campaignId "
            + "AND status = 'available' ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<PrizeSlotEntity> lockRandomAvailable(@Param("campaignId") long campaignId);

    /// 按档位统计奖池余量(只出份数,绝不带出 redeem_code / claimed_by)。
    @Query("SELECT s.amount AS amount, COUNT(s) AS total, "
            + "SUM(CASE WHEN s.status = 'available' THEN 1 ELSE 0 END) AS available "
            + "FROM PrizeSlotEntity s WHERE s.campaignId = :campaignId "
            + "GROUP BY s.amount ORDER BY s.amount")
    List<PoolTierView> poolByAmount(@Param("campaignId") long campaignId);

    /// 奖池档位投影。
    interface PoolTierView {
        BigDecimal getAmount();

        long getTotal();

        long getAvailable();
    }
}
