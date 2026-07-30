package me.supernb.activity.infra.adapter.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.ThursdayGuessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 猜桶竞猜仓库。
interface ThursdayGuessJpaRepository extends JpaRepository<ThursdayGuessEntity, Long> {

    Optional<ThursdayGuessEntity> findBySessionDateAndUserId(LocalDate sessionDate, long userId);

    /// 本场全部猜测,按提交时刻升序——并列时「先提交者胜」直接落在这个顺序上。
    List<ThursdayGuessEntity> findBySessionDateOrderByCreatedAtAsc(LocalDate sessionDate);

    long countBySessionDate(LocalDate sessionDate);
}
