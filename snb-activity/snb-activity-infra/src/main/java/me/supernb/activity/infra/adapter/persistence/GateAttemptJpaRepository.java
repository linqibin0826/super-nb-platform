package me.supernb.activity.infra.adapter.persistence;

import java.time.LocalDate;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.GateAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 金票每日抽签记录仓库。
interface GateAttemptJpaRepository extends JpaRepository<GateAttemptEntity, Long> {

    Optional<GateAttemptEntity> findByUserIdAndAttemptDate(long userId, LocalDate attemptDate);
}
