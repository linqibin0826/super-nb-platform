package me.supernb.activity.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/// activity.draw 行实体:一次抽奖记录(created_at 由 JPA 审计填充)。
@Entity
@Table(name = "draw", schema = "activity")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "user_id")
    private Long userId;

    /// 安慰奖为 NULL。
    @Column(name = "slot_id")
    private Long slotId;

    @Column(name = "amount")
    private BigDecimal amount;

    /// 安慰奖为 NULL。
    @Column(name = "redeem_code")
    private String redeemCode;

    @Column(name = "is_consolation")
    private boolean consolation;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public DrawEntity(long campaignId, long userId, Long slotId, BigDecimal amount,
                      String redeemCode, boolean consolation) {
        this.campaignId = campaignId;
        this.userId = userId;
        this.slotId = slotId;
        this.amount = amount;
        this.redeemCode = redeemCode;
        this.consolation = consolation;
    }
}
