package me.supernb.activity.domain.model.achievement;

import java.time.Instant;

/// 一条成就解锁记录(领域读视图)。
public record AchievementUnlock(long userId, String achievementCode, Instant unlockedAt, int pointsAtUnlock,
        String unlockSource, boolean seen) {
}
