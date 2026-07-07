package me.supernb.activity.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// activity.prize_slot 行实体。槽位由运营 SQL 预生成,JPA 侧只做「领取」状态翻转。
@Entity
@Table(name = "prize_slot", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PrizeSlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "redeem_code")
    private String redeemCode;

    @Column(name = "status")
    private String status;

    @Column(name = "claimed_by")
    private Long claimedBy;

    @Column(name = "claimed_at")
    private Instant claimedAt;

    /// 领取:置 claimed 并记录归属。只对事务内已锁行调用。
    public void claim(long userId, Instant at) {
        this.status = "claimed";
        this.claimedBy = userId;
        this.claimedAt = at;
    }
}
