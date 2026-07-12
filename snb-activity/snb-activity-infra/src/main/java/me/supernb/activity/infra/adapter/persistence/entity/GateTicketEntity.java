package me.supernb.activity.infra.adapter.persistence.entity;

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

/// 金票码池 JPA 实体,映射 `activity.gate_ticket`。
///
/// 码由站长灌入(runbook 27,真码绝不入 git);claimed_by 为空 = 库存,
/// 领取走 native SKIP LOCKED 选 id 后按实体置领取字段(池空即未中,预算硬封顶)。
@Entity
@Table(name = "gate_ticket", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GateTicketEntity extends BaseJpaEntity {

    /// 面额(元)。
    @Column(name = "amount")
    private BigDecimal amount;

    /// sub2api 兑换码原文(只回中签者本人,payload 纪律)。
    @Column(name = "code")
    private String code;

    /// 领取人(sub2api user id);空=库存。
    @Column(name = "claimed_by")
    private Long claimedBy;

    /// 领取时刻。
    @Column(name = "claimed_at")
    private Instant claimedAt;

    /// 构造:新码入池(测试/管线用;生产灌码走 SQL),雪花 id 显式预分配。
    public GateTicketEntity(BigDecimal amount, String code) {
        setId(SnowflakeIdGenerator.getId());
        this.amount = amount;
        this.code = code;
    }

    /// 领取:置领取人与时刻。
    public void claim(long userId, Instant at) {
        this.claimedBy = userId;
        this.claimedAt = at;
    }
}
