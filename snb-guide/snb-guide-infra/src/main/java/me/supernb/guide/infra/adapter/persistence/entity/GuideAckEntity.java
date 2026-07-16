package me.supernb.guide.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 引导已读标记,映射 `guide.guide_ack`((user_id, guide_key) 唯一)。
@Entity
@Table(name = "guide_ack", schema = "guide")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GuideAckEntity extends ChildJpaEntity {

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "guide_key", nullable = false)
    private String guideKey;

    /// 新建:雪花取号。
    public GuideAckEntity(long userId, String guideKey) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        this.guideKey = guideKey;
    }
}
