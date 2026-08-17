package me.supernb.ops.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;

/// 邮箱账号 JPA 实体,映射 `ops.account`。密码列恒存密文(加密在 app 层收口)。
@Entity
@Table(name = "account", schema = "ops")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpsAccountEntity extends BaseJpaEntity {

    @Column(nullable = false)
    private String email;

    private String provider;

    @Column(name = "password_enc")
    private String passwordEnc;

    @Column(name = "recovery_email")
    private String recoveryEmail;

    @Column(name = "recovery_password_enc")
    private String recoveryPasswordEnc;

    @Column(name = "reg_year")
    private String regYear;

    private String country;

    private String owner;

    /// AccountStatus.name()(条件校验在 app 层)。
    @Column(nullable = false)
    private String status;

    private String source;

    private String notes;

    /// 新建:雪花取号 + 全量赋值。
    public OpsAccountEntity(AccountData data) {
        setId(SnowflakeIdGenerator.getId());
        apply(data);
    }

    /// 全量覆盖(更新走同一份赋值,防漏字段)。
    public void apply(AccountData data) {
        this.email = data.email();
        this.provider = data.provider();
        this.passwordEnc = data.passwordEnc();
        this.recoveryEmail = data.recoveryEmail();
        this.recoveryPasswordEnc = data.recoveryPasswordEnc();
        this.regYear = data.regYear();
        this.country = data.country();
        this.owner = data.owner();
        this.status = data.status().name();
        this.source = data.source();
        this.notes = data.notes();
    }

    /// 还原成端口数据形状。
    public AccountData toData() {
        return new AccountData(email, provider, passwordEnc, recoveryEmail, recoveryPasswordEnc,
                regYear, country, owner, AccountStatus.valueOf(status), source, notes);
    }
}
