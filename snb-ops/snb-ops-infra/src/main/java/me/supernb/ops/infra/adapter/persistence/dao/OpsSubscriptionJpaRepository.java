package me.supernb.ops.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.ops.infra.adapter.persistence.entity.OpsSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 订阅 DAO。
public interface OpsSubscriptionJpaRepository extends JpaRepository<OpsSubscriptionEntity, Long> {

    List<OpsSubscriptionEntity> findByAccountIdOrderByCreatedAtAsc(long accountId);

    List<OpsSubscriptionEntity> findAllByOrderByCreatedAtDesc();

    int countByAccountId(long accountId);
}
