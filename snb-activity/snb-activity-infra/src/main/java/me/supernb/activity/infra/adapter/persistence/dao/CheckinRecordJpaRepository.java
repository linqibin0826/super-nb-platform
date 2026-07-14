package me.supernb.activity.infra.adapter.persistence.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// 签到记录仓库。
public interface CheckinRecordJpaRepository extends JpaRepository<CheckinRecordEntity, Long> {

    Optional<CheckinRecordEntity> findByUserIdAndCheckinDate(long userId, LocalDate checkinDate);

    int countByUserIdAndCheckinDateBetween(long userId, LocalDate from, LocalDate to);

    int countByUserId(long userId);

    List<CheckinRecordEntity> findByUserIdAndCheckinDateBetweenOrderByCheckinDateDesc(
            long userId, LocalDate from, LocalDate to);

    @Query("SELECT c.userId FROM CheckinRecordEntity c WHERE c.checkinDate BETWEEN :from AND :to "
            + "GROUP BY c.userId HAVING COUNT(c) = :expectedDays")
    List<Long> findFullAttendanceUserIds(@Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("expectedDays") long expectedDays);
}
