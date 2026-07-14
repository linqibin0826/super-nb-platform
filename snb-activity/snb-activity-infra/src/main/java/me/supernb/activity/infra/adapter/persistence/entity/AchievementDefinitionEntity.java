package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 成就目录条目 JPA 实体,映射 `activity.achievement_definition`。
///
/// 没有业务写路径(运维/发版才新增/退役行,同 campaign/category/prompt 惯例),
/// 不写业务构造器,只留受保护的无参构造器给 JPA。
@Entity
@Table(name = "achievement_definition", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AchievementDefinitionEntity extends BaseJpaEntity {

    @Column(name = "code")
    private String code;

    @Column(name = "series_code")
    private String seriesCode;

    @Column(name = "tier_level")
    private Integer tierLevel;

    @Column(name = "category")
    private String category;

    @Column(name = "rarity")
    private String rarity;

    @Column(name = "nb_points")
    private Integer nbPoints;

    @Column(name = "hidden_reveal")
    private boolean hiddenReveal;

    @Column(name = "always_private")
    private boolean alwaysPrivate;

    @Column(name = "status")
    private String status;

    @Column(name = "predicate_kind")
    private String predicateKind;

    @Column(name = "metric_code")
    private String metricCode;

    @Column(name = "threshold_value")
    private BigDecimal thresholdValue;

    @Column(name = "comparator")
    private String comparator;

    @Column(name = "prerequisite")
    private String prerequisite;

    @Column(name = "launch_date")
    private LocalDate launchDate;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "name")
    private String name;

    @Column(name = "condition_text")
    private String conditionText;

    @Column(name = "flavor_text")
    private String flavorText;

    @Column(name = "hidden_hint_text")
    private String hiddenHintText;
}
