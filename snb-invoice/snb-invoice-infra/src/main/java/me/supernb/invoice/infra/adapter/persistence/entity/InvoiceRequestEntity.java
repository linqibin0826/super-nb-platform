package me.supernb.invoice.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 发票申请 JPA 实体,映射 `invoice.invoice_request`。聚合根;状态转移不走实体脏检查,
/// 一律 DAO 守卫式 UPDATE(见 InvoiceRequestJpaRepository),实体只负责创建时落快照。
@Entity
@Table(name = "invoice_request", schema = "invoice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceRequestEntity extends BaseJpaEntity {

    /// 对外单号 = "INV"+雪花 id(幂等 notes 的业务辨识键)。
    @Column(name = "request_no", nullable = false, unique = true)
    private String requestNo;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal fee;

    @Column(nullable = false)
    private String status;

    @Column(name = "profile_type", nullable = false)
    private String profileType;

    @Column(name = "profile_title", nullable = false)
    private String profileTitle;

    @Column(name = "profile_tax_no")
    private String profileTaxNo;

    @Column(name = "profile_reg_address")
    private String profileRegAddress;

    @Column(name = "profile_reg_phone")
    private String profileRegPhone;

    @Column(name = "profile_bank_name")
    private String profileBankName;

    @Column(name = "profile_bank_account")
    private String profileBankAccount;

    /// 提交那一刻抬头的核验章快照(之后改抬头不影响本单)。
    @Column(name = "profile_verified_at")
    private Instant profileVerifiedAt;

    @Column
    private String remark;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "fee_charged_at")
    private Instant feeChargedAt;

    @Column(name = "issued_at")
    private Instant issuedAt;

    /// 新建:雪花取号、单号派生、抬头快照独立列(含核验章)、初始 PENDING。
    public InvoiceRequestEntity(long userId, BigDecimal amount, BigDecimal fee, ProfileData profile,
            Instant profileVerifiedAt, String remark) {
        setId(SnowflakeIdGenerator.getId());
        this.requestNo = "INV" + getId();
        this.userId = userId;
        this.amount = amount;
        this.fee = fee;
        this.status = InvoiceStatus.PENDING.name();
        this.profileType = profile.type().name();
        this.profileTitle = profile.title();
        this.profileTaxNo = profile.taxNo();
        this.profileRegAddress = profile.regAddress();
        this.profileRegPhone = profile.regPhone();
        this.profileBankName = profile.bankName();
        this.profileBankAccount = profile.bankAccount();
        this.profileVerifiedAt = profileVerifiedAt;
        this.remark = remark;
    }
}
