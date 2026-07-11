package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// `activity.raffle_entry` 仓储。
public interface RaffleEntryJpaRepository extends JpaRepository<RaffleEntryEntity, Long> {

    /// 报名序列化锁(事务级,随提交/回滚自动释放):campaign 维度取号防并发撞 entry_no。
    /// 用 hashtextextended 把 key 命名空间化,避免与 draw 切片的 userId 单参锁在同一键空间互撞。
    @Query(value = "SELECT true FROM (SELECT pg_advisory_xact_lock("
            + "hashtextextended('raffle_entry_' || CAST(:cid AS text), 0))) AS acquired", nativeQuery = true)
    boolean acquireCampaignXactLock(@Param("cid") long campaignId);

    /// 本人在该期的报名记录(幂等判定)。
    Optional<RaffleEntryEntity> findByCampaignIdAndUserId(long campaignId, long userId);

    /// 该期报名人数。
    int countByCampaignId(long campaignId);

    /// 最近报名的 12 条(列席名单滚动展示)。
    List<RaffleEntryEntity> findTop12ByCampaignIdOrderByCreatedAtDesc(long campaignId);

    /// 该期全量报名(开奖复核+抽样用)。
    List<RaffleEntryEntity> findByCampaignId(long campaignId);
}
