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

/// activity.campaign 行实体。活动由运营 SQL 预置,JPA 侧只读。
@Entity
@Table(name = "campaign", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "status")
    private String status;

    @Column(name = "consolation_amount")
    private BigDecimal consolationAmount;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
