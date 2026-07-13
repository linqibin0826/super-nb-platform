package me.supernb.activity.infra.adapter.persistence.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// 签到记录仓库。
public interface CheckinRecordJpaRepository extends JpaRepository<CheckinRecordEntity, Long> {

    Optional<CheckinRecordEntity> findByUserIdAndCheckinDate(long userId, LocalDate checkinDate);

    int countByUserIdAndCheckinDateBetween(long userId, LocalDate from, LocalDate to);

    int countByUserId(long userId);

    List<CheckinRecordEntity> findByUserIdAndCheckinDateBetweenOrderByCheckinDateDesc(
            long userId, LocalDate from, LocalDate to);
}
