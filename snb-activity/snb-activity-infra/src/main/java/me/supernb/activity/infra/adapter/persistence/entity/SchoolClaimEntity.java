package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 开学季领取台账 JPA 实体,映射 `activity.school_claim`。
///
/// `(user_id, kind, tier)` 唯一键是「一档只领一次」的并发仲裁真源;
/// grant_status 状态机 pending → success | failed(failed 可重试)。
@Entity
@Table(name = "school_claim", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchoolClaimEntity extends BaseJpaEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "kind")
    private String kind;

    @Column(name = "tier")
    private int tier;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "grant_status")
    private String grantStatus;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    /// 标记发卡成功。
    public void markSuccess() {
        this.grantStatus = "success";
    }

    /// 标记发卡失败:累加尝试次数,记录错误信息。
    public void markFailed(String error) {
        this.grantStatus = "failed";
        this.attempts++;
        this.lastError = error;
    }
}
