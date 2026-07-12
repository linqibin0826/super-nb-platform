package me.supernb.activity.infra.adapter.persistence;

import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.GateTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/// 金票码池仓库。
interface GateTicketJpaRepository extends JpaRepository<GateTicketEntity, Long> {

    /// 随机取一张库存码并行级锁定(SKIP LOCKED:并发领取互不阻塞、绝不重选同一张)。
    @Query(value = "SELECT id FROM activity.gate_ticket WHERE claimed_by IS NULL "
            + "ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    Optional<Long> pickAvailableId();
}
