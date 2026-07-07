package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.ChildJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 奖槽 JPA 实体,映射 `activity.prize_slot`。
///
/// campaign 聚合的子实体,继承 [ChildJpaEntity](领奖是独立更新语义,乐观锁列随身);
/// 槽位由运维 SQL 预生成,应用侧不写业务构造器,只经 `FOR UPDATE SKIP LOCKED` 认领。
@Entity
@Table(name = "prize_slot", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrizeSlotEntity extends ChildJpaEntity {

    /// 所属活动 id。
    @Column(name = "campaign_id")
    private Long campaignId;

    /// 奖槽金额(元)。
    @Column(name = "amount")
    private BigDecimal amount;

    /// 预生成的 sub2api balance 兑换码。
    @Column(name = "redeem_code")
    private String redeemCode;

    /// 状态:`available` | `claimed`。
    @Column(name = "status")
    private String status;

    /// 领奖用户(sub2api user id),未领为 NULL。
    @Column(name = "claimed_by")
    private Long claimedBy;

    /// 领奖时刻,未领为 NULL。
    @Column(name = "claimed_at")
    private Instant claimedAt;

    /// 把本槽置为已领:记录领奖用户与时刻,状态转 `claimed`。
    public void claim(long userId, Instant at) {
        this.status = "claimed";
        this.claimedBy = userId;
        this.claimedAt = at;
    }
}
