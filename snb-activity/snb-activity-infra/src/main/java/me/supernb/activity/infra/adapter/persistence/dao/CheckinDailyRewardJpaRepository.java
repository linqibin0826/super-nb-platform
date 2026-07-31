package me.supernb.activity.infra.adapter.persistence.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinDailyRewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// 每日返网费台账仓库。
public interface CheckinDailyRewardJpaRepository extends JpaRepository<CheckinDailyRewardEntity, Long> {

    Optional<CheckinDailyRewardEntity> findByUserIdAndCheckinDate(long userId, LocalDate checkinDate);

    /// 可重试的行:还在 pending(崩溃恢复/超时) 或已 failed 但尝试次数未耗尽。
    @Query("SELECT r FROM CheckinDailyRewardEntity r WHERE r.balanceStatus IN ('pending','failed') "
            + "AND r.attempts < :maxAttempts")
    List<CheckinDailyRewardEntity> findRetryable(@Param("maxAttempts") int maxAttempts);
}
