package me.supernb.activity.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.activity.infra.adapter.persistence.entity.AchievementUnlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AchievementUnlockJpaRepository extends JpaRepository<AchievementUnlockEntity, Long> {

    List<AchievementUnlockEntity> findByUserIdAndRevokedAtIsNull(long userId);

    int countByUserIdAndRevokedAtIsNull(long userId);

    @Query("SELECT a.achievementCode FROM AchievementUnlockEntity a "
            + "WHERE a.userId = :userId AND a.revokedAt IS NULL")
    List<String> findUnlockedCodes(@Param("userId") long userId);

    List<AchievementUnlockEntity> findByUserIdAndAchievementCodeIn(long userId, List<String> codes);
}
