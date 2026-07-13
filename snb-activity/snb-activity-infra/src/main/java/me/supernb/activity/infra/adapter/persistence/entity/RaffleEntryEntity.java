package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 报名(列席)JPA 实体,映射 `activity.raffle_entry`。
///
/// 聚合根,继承 [BaseJpaEntity];created_by 由 JPA 审计自动填=报名用户本人。
@Entity
@Table(name = "raffle_entry", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleEntryEntity extends BaseJpaEntity {

    /// 所属期 id。
    @Column(name = "campaign_id")
    private Long campaignId;

    /// 报名用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 参会证号(期内连续,campaign 级 advisory lock 下取号)。
    @Column(name = "entry_no")
    private int entryNo;

    /// 报名时点门槛指标值(留痕;开奖以复核为准)。
    @Column(name = "gate_value_at_entry")
    private BigDecimal gateValueAtEntry;

    /// 报名请求来源 IP(X-Forwarded-For 末值=Caddy 亲验真实对端,秋后清算用;首值可伪造,见 deployment/31)。
    @Column(name = "client_ip")
    private String clientIp;

    /// 报名请求 UA。
    @Column(name = "user_agent")
    private String userAgent;

    /// 构造:新报名记录,雪花 id 在此显式预分配。
    public RaffleEntryEntity(long campaignId, long userId, int entryNo, BigDecimal gateValueAtEntry,
                             String clientIp, String userAgent) {
        setId(SnowflakeIdGenerator.getId());
        this.campaignId = campaignId;
        this.userId = userId;
        this.entryNo = entryNo;
        this.gateValueAtEntry = gateValueAtEntry;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
}
