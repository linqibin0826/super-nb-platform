package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 收藏成员 JPA 实体,映射 `gallery.prompt_favorite`。
///
/// prompt 聚合的子实体,继承 [ChildJpaEntity];雪花代理主键 +
/// `UNIQUE(prompt_id, user_id)` 保幂等;created_at 由审计填充,
/// 即收藏时刻(「我的收藏」按它排序)。
@Entity
@Table(name = "prompt_favorite", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptFavoriteEntity extends ChildJpaEntity {

    /// 被收藏的提示词 id。
    @Column(name = "prompt_id")
    private Long promptId;

    /// 收藏用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 构造:新收藏成员,雪花 id 应用层预分配。
    public PromptFavoriteEntity(long promptId, long userId) {
        setId(SnowflakeIdGenerator.getId());
        this.promptId = promptId;
        this.userId = userId;
    }
}
