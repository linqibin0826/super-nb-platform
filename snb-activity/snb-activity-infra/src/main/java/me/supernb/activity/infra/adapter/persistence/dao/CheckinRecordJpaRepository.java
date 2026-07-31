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

    /// 区间内累计签到 ≥ minDays 天的用户 id(加时资格判定;2026-07-31 由 `= expectedDays` 放宽而来)。
    @Query("SELECT c.userId FROM CheckinRecordEntity c WHERE c.checkinDate BETWEEN :from AND :to "
            + "GROUP BY c.userId HAVING COUNT(c) >= :minDays")
    List<Long> findUserIdsWithAtLeastDays(@Param("from") LocalDate from, @Param("to") LocalDate to,
            @Param("minDays") long minDays);

    /// 某天打卡了的全部用户 id(返网费补偿 job 补录当日缺失台账用)。
    @Query("SELECT c.userId FROM CheckinRecordEntity c WHERE c.checkinDate = :day")
    List<Long> findUserIdsByCheckinDate(@Param("day") LocalDate day);
}
