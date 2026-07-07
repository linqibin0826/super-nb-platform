package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.activity.infra.adapter.persistence.entity.DrawEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// `activity.draw` 仓储。
public interface DrawJpaRepository extends JpaRepository<DrawEntity, Long> {

    /// 事务级 advisory lock 串行化同一用户(随事务结束自动释放),防并发超额;
    /// pg_advisory_xact_lock 返回 void 无法直接作标量映射,包一层子查询取 boolean。
    @Query(value = "SELECT true FROM (SELECT pg_advisory_xact_lock(:key)) AS acquired", nativeQuery = true)
    boolean acquireUserXactLock(@Param("key") long key);

    /// 该活动内该用户的已抽次数。
    int countByCampaignIdAndUserId(long campaignId, long userId);

    /// 该活动内该用户的抽奖记录,按创建时间倒序,至多 100 条。
    List<DrawEntity> findTop100ByCampaignIdAndUserIdOrderByCreatedAtDesc(long campaignId, long userId);

    /// 该活动排除安慰奖的中奖记录,按创建时间倒序,取 limit 条。
    List<DrawEntity> findByCampaignIdAndConsolationFalseOrderByCreatedAtDesc(long campaignId, Limit limit);
}
