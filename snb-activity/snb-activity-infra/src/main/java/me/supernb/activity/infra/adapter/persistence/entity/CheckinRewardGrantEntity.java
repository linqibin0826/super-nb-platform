package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 补给资格发放台账 JPA 实体,映射 `activity.checkin_reward_grant`。
///
/// `(user_id, grant_month)` 唯一键是"每人每自然月限领 1 档"的并发仲裁真源,兼作
/// bulk-assign 幂等占位;status 状态机 pending → success | failed | deferred。
@Entity
@Table(name = "checkin_reward_grant", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckinRewardGrantEntity extends BaseJpaEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "grant_month")
    private LocalDate grantMonth;

    @Column(name = "tier")
    private String tier;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "status")
    private String status;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "notes")
    private String notes;

    @Column(name = "last_error")
    private String lastError;

    /// 构造:占位插入(测试/兜底用;生产写路径经 CheckinRewardAdapter 的原生
    /// INSERT ON CONFLICT DO NOTHING RETURNING 语句,不经本构造器),雪花 id 显式预分配。
    public CheckinRewardGrantEntity(long userId, LocalDate grantMonth, String tier, long groupId, String notes) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        this.grantMonth = grantMonth;
        this.tier = tier;
        this.groupId = groupId;
        this.status = "pending";
        this.attempts = 0;
        this.notes = notes;
    }

    /// 标记发放成功。
    public void markSuccess() {
        this.status = "success";
    }

    /// 标记发放失败:累加尝试次数,记录错误信息。
    public void markFailed(String error) {
        this.status = "failed";
        this.attempts++;
        this.lastError = error;
    }

    /// 标记预算硬顶打满,显式转入排队。
    public void markDeferred() {
        this.status = "deferred";
    }
}
