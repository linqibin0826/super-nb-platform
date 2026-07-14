package me.supernb.activity.infra.adapter.persistence;

import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.supernb.activity.domain.model.achievement.AchievementUnlock;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import me.supernb.activity.infra.adapter.persistence.dao.AchievementUnlockJpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// AchievementUnlockPort 实现:解锁写路径用原生 INSERT ON CONFLICT DO NOTHING(无需 advisory
/// lock——只有事实判断,没有稀缺资源分配,深化稿 §6.2 明确说明);已读回执走 JPA dirty checking。
@Repository
public class AchievementUnlockAdapter implements AchievementUnlockPort {

    private final AchievementUnlockJpaRepository repo;
    private final JdbcTemplate jdbc;

    /// 构造:注入解锁台账仓库与 Boot 主数据源的 JdbcTemplate。
    public AchievementUnlockAdapter(AchievementUnlockJpaRepository repo, JdbcTemplate jdbc) {
        this.repo = repo;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public boolean unlock(long userId, String achievementCode, Instant unlockedAt, int pointsAtUnlock,
            String unlockSource) {
        long id = SnowflakeIdGenerator.getId();
        List<Long> inserted = jdbc.query(
                "INSERT INTO activity.achievement_unlock "
                        + "(id, user_id, achievement_code, unlocked_at, points_at_unlock, unlock_source) "
                        + "VALUES (?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (user_id, achievement_code) DO NOTHING RETURNING id",
                (rs, i) -> rs.getLong("id"),
                id, userId, achievementCode, Timestamp.from(unlockedAt), pointsAtUnlock, unlockSource);
        return !inserted.isEmpty();
    }

    @Override
    public Set<String> unlockedCodes(long userId) {
        return new HashSet<>(repo.findUnlockedCodes(userId));
    }

    @Override
    public List<AchievementUnlock> myUnlocks(long userId) {
        return repo.findByUserIdAndRevokedAtIsNull(userId).stream()
                .map(e -> new AchievementUnlock(e.getUserId(), e.getAchievementCode(), e.getUnlockedAt(),
                        e.getPointsAtUnlock(), e.getUnlockSource(), e.isSeen()))
                .toList();
    }

    @Override
    public int unlockedCount(long userId) {
        return repo.countByUserIdAndRevokedAtIsNull(userId);
    }

    @Override
    @Transactional
    public int markSeen(long userId, List<String> codes) {
        var entities = repo.findByUserIdAndAchievementCodeIn(userId, codes);
        Instant now = Instant.now();
        int updated = 0;
        for (var e : entities) {
            if (!e.isSeen()) {
                e.markSeen(now);
                updated++;
            }
        }
        return updated;
    }
}
