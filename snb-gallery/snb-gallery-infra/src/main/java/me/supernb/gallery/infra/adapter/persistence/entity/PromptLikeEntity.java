package me.supernb.gallery.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 点赞成员 JPA 实体,映射 `gallery.prompt_like`。
///
/// prompt 聚合的子实体,继承 [ChildJpaEntity];雪花代理主键 +
/// `UNIQUE(prompt_id, user_id)` 保幂等(撞唯一约束→事务回滚→外层回读计数);
/// created_at 由审计填充,即点赞时刻(「我的点赞」按它排序)。
@Entity
@Table(name = "prompt_like", schema = "gallery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PromptLikeEntity extends ChildJpaEntity {

    /// 被点赞的提示词 id。
    @Column(name = "prompt_id")
    private Long promptId;

    /// 点赞用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 构造:新点赞成员,雪花 id 应用层预分配。
    public PromptLikeEntity(long promptId, long userId) {
        setId(SnowflakeIdGenerator.getId());
        this.promptId = promptId;
        this.userId = userId;
    }
}
