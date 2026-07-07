package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 活动 JPA 实体,映射 `activity.campaign`。
///
/// 聚合根,继承 [BaseJpaEntity] 获得雪花 id 与全套审计列;
/// 活动由运维 SQL 创建与收尾,应用侧只读,不写业务构造器。
@Entity
@Table(name = "campaign", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignEntity extends BaseJpaEntity {

    /// 活动名称。
    @Column(name = "name")
    private String name;

    /// 生效起点(含)。
    @Column(name = "starts_at")
    private Instant startsAt;

    /// 生效终点(排他上界)。
    @Column(name = "ends_at")
    private Instant endsAt;

    /// 状态:`active` | `ended`。
    @Column(name = "status")
    private String status;

    /// 安慰奖金额(元;奖池抽空后的兜底额度)。
    @Column(name = "consolation_amount")
    private BigDecimal consolationAmount;
}
