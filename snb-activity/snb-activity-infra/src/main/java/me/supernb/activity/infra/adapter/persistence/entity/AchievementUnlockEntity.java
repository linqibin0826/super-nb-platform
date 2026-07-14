package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 成就解锁记录 JPA 实体,映射 `activity.achievement_unlock`。继承 `ChildJpaEntity`
/// (4 字段:id/created_at/updated_at/version,已核实 patra 源码——无 created_by/
/// record_remarks/ip_address):系统批处理写入,没有"created_by 用户"这个概念,
/// 呼应深化稿"achievement_unlock 是追加写入的不可变事实流水"。
@Entity
@Table(name = "achievement_unlock", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AchievementUnlockEntity extends ChildJpaEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "achievement_code")
    private String achievementCode;

    @Column(name = "unlocked_at")
    private Instant unlockedAt;

    @Column(name = "points_at_unlock")
    private Integer pointsAtUnlock;

    @Column(name = "unlock_source")
    private String unlockSource;

    @Column(name = "seen")
    private boolean seen;

    @Column(name = "seen_at")
    private Instant seenAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /// 构造:一条解锁事实,雪花 id 显式预分配(生产写路径经原生 INSERT ON CONFLICT DO NOTHING,
    /// 不经本构造器,详见 AchievementUnlockAdapter;本构造器供测试直接落库使用)。
    public AchievementUnlockEntity(long userId, String achievementCode, Instant unlockedAt,
            int pointsAtUnlock, String unlockSource) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        this.achievementCode = achievementCode;
        this.unlockedAt = unlockedAt;
        this.pointsAtUnlock = pointsAtUnlock;
        this.unlockSource = unlockSource;
        this.seen = false;
    }

    /// 标记已读。
    public void markSeen(Instant at) {
        this.seen = true;
        this.seenAt = at;
    }
}
