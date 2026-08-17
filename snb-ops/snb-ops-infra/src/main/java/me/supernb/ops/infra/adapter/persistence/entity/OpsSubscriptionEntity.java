package me.supernb.ops.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.model.SubTier;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;

/// 服务开通/订阅 JPA 实体,映射 `ops.subscription`(注册→付费→封号→退款一行装下)。
@Entity
@Table(name = "subscription", schema = "ops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpsSubscriptionEntity extends BaseJpaEntity {

    @Column(name = "account_id", nullable = false)
    private long accountId;

    /// SubService.name()。
    @Column(nullable = false)
    private String service;

    private String tier;

    private String region;

    @Column(name = "card_platform")
    private String cardPlatform;

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "register_ip")
    private String registerIp;

    @Column(name = "current_ip")
    private String currentIp;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @Column(name = "started_at")
    private LocalDate startedAt;

    @Column(name = "next_billing_at")
    private LocalDate nextBillingAt;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    /// SubStatus.name()(BANNED 必带 bannedAt,校验在 app 层)。
    @Column(nullable = false)
    private String status;

    @Column(name = "sub2api_account_id")
    private Long sub2apiAccountId;

    @Column(name = "sub2api_account_name")
    private String sub2apiAccountName;

    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "banned_while_paid")
    private Boolean bannedWhilePaid;

    /// RefundStatus.name()(app 层保证非空,缺省 NONE)。
    @Column(name = "refund_status", nullable = false)
    private String refundStatus;

    @Column(name = "refund_amount_usd")
    private BigDecimal refundAmountUsd;

    @Column(name = "appealed_at")
    private LocalDate appealedAt;

    @Column(name = "refund_resolved_at")
    private LocalDate refundResolvedAt;

    @Column(name = "refund_follow_up_at")
    private LocalDate refundFollowUpAt;

    @Column(name = "refund_notes")
    private String refundNotes;

    private String notes;

    /// 新建:雪花取号 + 全量赋值。
    public OpsSubscriptionEntity(SubscriptionData data) {
        setId(SnowflakeIdGenerator.getId());
        apply(data);
    }

    /// 全量覆盖(更新走同一份赋值,防漏字段)。
    public void apply(SubscriptionData data) {
        this.accountId = data.accountId();
        this.service = data.service().name();
        this.tier = data.tier() == null ? null : data.tier().name();
        this.region = data.region();
        this.cardPlatform = data.cardPlatform();
        this.cardLast4 = data.cardLast4();
        this.registerIp = data.registerIp();
        this.currentIp = data.currentIp();
        this.registeredAt = data.registeredAt();
        this.startedAt = data.startedAt();
        this.nextBillingAt = data.nextBillingAt();
        this.priceUsd = data.priceUsd();
        this.status = data.status().name();
        this.sub2apiAccountId = data.sub2apiAccountId();
        this.sub2apiAccountName = data.sub2apiAccountName();
        this.bannedAt = data.bannedAt();
        this.bannedWhilePaid = data.bannedWhilePaid();
        this.refundStatus = data.refundStatus().name();
        this.refundAmountUsd = data.refundAmountUsd();
        this.appealedAt = data.appealedAt();
        this.refundResolvedAt = data.refundResolvedAt();
        this.refundFollowUpAt = data.refundFollowUpAt();
        this.refundNotes = data.refundNotes();
        this.notes = data.notes();
    }

    /// 还原成端口数据形状。
    public SubscriptionData toData() {
        return new SubscriptionData(accountId, SubService.valueOf(service),
                tier == null ? null : SubTier.valueOf(tier), region, cardPlatform, cardLast4,
                registerIp, currentIp, registeredAt, startedAt, nextBillingAt, priceUsd,
                SubStatus.valueOf(status), sub2apiAccountId, sub2apiAccountName, bannedAt, bannedWhilePaid,
                RefundStatus.valueOf(refundStatus), refundAmountUsd, appealedAt, refundResolvedAt,
                refundFollowUpAt, refundNotes, notes);
    }
}
