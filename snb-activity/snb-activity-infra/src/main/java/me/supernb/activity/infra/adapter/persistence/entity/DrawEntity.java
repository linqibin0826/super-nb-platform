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

/// 抽奖记录 JPA 实体,映射 `activity.draw`。
///
/// 聚合根,继承 [BaseJpaEntity];created_at/created_by 由 JPA 审计自动填充
/// (created_by 即发起抽奖的登录用户,见 boot 的 `auditorAware` 装配)。
@Entity
@Table(name = "draw", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawEntity extends BaseJpaEntity {

    /// 所属活动 id。
    @Column(name = "campaign_id")
    private Long campaignId;

    /// 抽奖用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 命中的奖槽 id,安慰奖为 NULL。
    @Column(name = "slot_id")
    private Long slotId;

    /// 本次抽得金额。
    @Column(name = "amount")
    private BigDecimal amount;

    /// 兑换码,安慰奖为 NULL(人工发放)。
    @Column(name = "redeem_code")
    private String redeemCode;

    /// 是否安慰奖。
    @Column(name = "is_consolation")
    private boolean consolation;

    /// 构造:新抽奖记录,雪花 id 应用层预分配。
    public DrawEntity(long campaignId, long userId, Long slotId, BigDecimal amount,
                      String redeemCode, boolean consolation) {
        setId(SnowflakeIdGenerator.getId());
        this.campaignId = campaignId;
        this.userId = userId;
        this.slotId = slotId;
        this.amount = amount;
        this.redeemCode = redeemCode;
        this.consolation = consolation;
    }
}
