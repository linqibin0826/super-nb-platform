package me.supernb.activity.infra.jpa;

import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// activity.draw 仓储。
public interface DrawJpaRepository extends JpaRepository<DrawEntity, Long> {

    /// 事务级 advisory lock 串行化同一用户(随事务结束自动释放),防并发超额。
    /// pg_advisory_xact_lock 返回 void 无法作标量映射,包一层子查询取 boolean。
    @Query(value = "SELECT true FROM (SELECT pg_advisory_xact_lock(:key)) AS acquired", nativeQuery = true)
    boolean acquireUserXactLock(@Param("key") long key);

    int countByCampaignIdAndUserId(long campaignId, long userId);

    List<DrawEntity> findTop100ByCampaignIdAndUserIdOrderByCreatedAtDesc(long campaignId, long userId);

    List<DrawEntity> findByCampaignIdAndConsolationFalseOrderByCreatedAtDesc(long campaignId, Limit limit);
}
