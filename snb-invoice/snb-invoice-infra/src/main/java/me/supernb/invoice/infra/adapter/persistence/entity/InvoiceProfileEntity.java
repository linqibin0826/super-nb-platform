package me.supernb.invoice.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;

/// 开票抬头 JPA 实体,映射 `invoice.invoice_profile`。聚合根,继承 BaseJpaEntity(全套审计列)。
@Entity
@Table(name = "invoice_profile", schema = "invoice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvoiceProfileEntity extends BaseJpaEntity {

    @Column(name = "user_id", nullable = false)
    private long userId;

    /// COMPANY / PERSONAL(条件校验在 app 层)。
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(name = "tax_no")
    private String taxNo;

    @Column(name = "reg_address")
    private String regAddress;

    @Column(name = "reg_phone")
    private String regPhone;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    /// 核验章:名称+税号与官方开票资料一致的时刻;null=未核验。取值判定在 app 层 ProfileStamp。
    @Column(name = "verified_at")
    private Instant verifiedAt;

    /// 新建:雪花取号 + 全量赋值(含章)。
    public InvoiceProfileEntity(long userId, ProfileData data, Instant verifiedAt) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        apply(data, verifiedAt);
    }

    /// 全量覆盖(更新走同一份赋值,防漏字段);章随内容一起覆盖,不提供单改内容的入口。
    public void apply(ProfileData data, Instant verifiedAt) {
        this.type = data.type().name();
        this.title = data.title();
        this.taxNo = data.taxNo();
        this.regAddress = data.regAddress();
        this.regPhone = data.regPhone();
        this.bankName = data.bankName();
        this.bankAccount = data.bankAccount();
        this.verifiedAt = verifiedAt;
    }
}
