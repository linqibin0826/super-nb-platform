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
        if (inserted.isEmpty()) {
            return false;
        }
        // 解锁即记账(同一事务,对齐「解锁即既得」;真源=账本,points_at_unlock 保留作成就域展示):
        // 账本行 id 复用解锁行 id(与 V11 补铸同口径);points=0 防御跳过(CHECK 要求 EARN>0)
        if (pointsAtUnlock > 0) {
            jdbc.update("INSERT INTO activity.nb_ledger "
                            + "(id, user_id, entry_type, source_type, source_ref, points, occurred_at) "
                            + "VALUES (?, ?, 'EARN', 'achievement_unlock', ?, ?, ?) "
                            + "ON CONFLICT (user_id, source_type, source_ref) DO NOTHING",
                    inserted.get(0), userId, achievementCode, pointsAtUnlock, Timestamp.from(unlockedAt));
        }
        return true;
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
