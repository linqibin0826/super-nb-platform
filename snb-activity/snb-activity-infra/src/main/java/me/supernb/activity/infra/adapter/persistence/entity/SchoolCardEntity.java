package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 包机邀请卡 JPA 实体,映射 `activity.school_card`(玩法 v2,一人一张)。
///
/// user_id 唯一键是开卡并发仲裁真源;resets_used 的原子扣减走 JDBC 条件 UPDATE
/// (见 SchoolCardAdapter),本实体只承担读路径与升档回写。
@Entity
@Table(name = "school_card", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolCardEntity extends BaseJpaEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "tier")
    private int tier;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "resets_used")
    private int resetsUsed;

    /// 升档:换组重发后更新档位与新订阅 id。
    public void upgrade(int tier, long subscriptionId) {
        this.tier = tier;
        this.subscriptionId = subscriptionId;
    }
}
