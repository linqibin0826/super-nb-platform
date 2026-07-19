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

/// 奖品件 JPA 实体,映射 `activity.raffle_prize`(一件一行)。
///
/// campaign 聚合子实体,继承 [ChildJpaEntity](归属写入是独立更新语义);
/// 奖品通过管理端点新增/编辑/删除(见 RaffleAdminController),开奖事务另经 assign 归属。
/// ⚠️ payload 是机密:实体可读,但任何公开读视图/响应 DTO 不得携带该字段。
@Entity
@Table(name = "raffle_prize", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RafflePrizeEntity extends ChildJpaEntity {

    /// 所属期 id。
    @Column(name = "campaign_id")
    private Long campaignId;

    /// 档位(S/A/B/C/D 自由文本)。
    @Column(name = "tier")
    private String tier;

    /// 官腔展示名(含真实奖品与面值)。
    @Column(name = "display_name")
    private String displayName;

    /// 奖品形态:REDEEM_CODE | ALIPAY_CODE。
    @Column(name = "kind")
    private String kind;

    /// 机密:sub2api 兑换码或支付宝口令明文。
    @Column(name = "payload")
    private String payload;

    /// 张榜/分配顺序,大奖靠前。
    @Column(name = "sort_order")
    private int sortOrder;

    /// 中奖用户,NULL=无主。
    @Column(name = "winner_user_id")
    private Long winnerUserId;

    /// 归属时刻。
    @Column(name = "assigned_at")
    private Instant assignedAt;

    /// 管理端新增一件奖品(手填或生成后落库)。
    public RafflePrizeEntity(long campaignId, String tier, String displayName, String kind,
            String payload, int sortOrder) {
        setId(SnowflakeIdGenerator.getId());
        this.campaignId = campaignId;
        this.tier = tier;
        this.displayName = displayName;
        this.kind = kind;
        this.payload = payload;
        this.sortOrder = sortOrder;
    }

    /// 管理端编辑本件(开奖前;调用方负责校验所属期 status=="active")。
    public void update(String tier, String displayName, String kind, String payload, int sortOrder) {
        this.tier = tier;
        this.displayName = displayName;
        this.kind = kind;
        this.payload = payload;
        this.sortOrder = sortOrder;
    }

    /// 仅回填 payload(tier/displayName/sortOrder 不动)。
    public void updatePayload(String payload) {
        this.payload = payload;
    }

    /// 开奖事务内把本件归属给中奖者。
    public void assign(long userId, Instant at) {
        this.winnerUserId = userId;
        this.assignedAt = at;
    }
}
