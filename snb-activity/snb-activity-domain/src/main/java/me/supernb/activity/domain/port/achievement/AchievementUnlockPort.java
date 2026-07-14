package me.supernb.activity.domain.port.achievement;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import me.supernb.activity.domain.model.achievement.AchievementUnlock;

/// 成就解锁台账端口:写读合一,照 CheckinPort/RaffleEntryPort 惯例。
public interface AchievementUnlockPort {

    /// 解锁一条成就(INSERT ON CONFLICT DO NOTHING 天然幂等,已解锁不重复写);
    /// 返回是否真插入(true = 本次新解锁,由调用方决定是否触发仪式/统计增量)。
    boolean unlock(long userId, String achievementCode, Instant unlockedAt, int pointsAtUnlock, String unlockSource);

    /// 用户已解锁的成就 code 集合(不含 revoked,判定引擎去重候选用)。
    Set<String> unlockedCodes(long userId);

    /// 用户全部解锁记录(不含 revoked,成就墙查询用)。
    List<AchievementUnlock> myUnlocks(long userId);

    /// 用户已解锁成就总数(不含 revoked;meta_regular"熟客认证"判定 & 墙面 summary 复用)。
    int unlockedCount(long userId);

    /// 批量标记已读,返回实际更新的行数。
    int markSeen(long userId, List<String> codes);
}
