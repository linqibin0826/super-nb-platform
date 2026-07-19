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

/// 发布会活动 JPA 实体,映射 `activity.raffle_campaign`。
///
/// 聚合根,继承 [BaseJpaEntity];期通过管理端点创建/编辑(见 RaffleAdminController)。
/// 开奖侧的状态翻转与留痕统计走 DAO 原生 UPDATE(CAS 语义),不经实体 mutator。
@Entity
@Table(name = "raffle_campaign", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RaffleCampaignEntity extends BaseJpaEntity {

    /// 期次名。
    @Column(name = "name")
    private String name;

    /// 报名开放时刻(含)。
    @Column(name = "entry_open_at")
    private Instant entryOpenAt;

    /// 报名截止时刻(排他上界)。
    @Column(name = "entry_close_at")
    private Instant entryCloseAt;

    /// 开奖时刻。
    @Column(name = "draw_at")
    private Instant drawAt;

    /// 门槛类型:RECHARGE | SPEND。
    @Column(name = "gate_type")
    private String gateType;

    /// 门槛金额(元)。
    @Column(name = "gate_amount")
    private BigDecimal gateAmount;

    /// 门槛起算时刻。
    @Column(name = "gate_from")
    private Instant gateFrom;

    /// 可选账龄门槛(注册满 N 天),NULL=不限。
    @Column(name = "min_account_age_days")
    private Integer minAccountAgeDays;

    /// 权重模式:EQUAL | WEIGHTED。
    @Column(name = "weight_mode")
    private String weightMode;

    /// 状态:active | drawn | cancelled。
    @Column(name = "status")
    private String status;

    /// 开奖时刻留痕(CAS UPDATE 服务端 now() 写入)。
    @Column(name = "drawn_at")
    private Instant drawnAt;

    /// 开奖时报名人数留痕。
    @Column(name = "entrant_count_at_draw")
    private Integer entrantCountAtDraw;

    /// 开奖复核取消资格人数留痕。
    @Column(name = "disqualified_count")
    private Integer disqualifiedCount;

    /// 管理端新建一期:status 固定 active,开奖留痕三列留空待开奖任务填充。
    public RaffleCampaignEntity(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            String gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
            String weightMode) {
        setId(SnowflakeIdGenerator.getId());
        this.name = name;
        this.entryOpenAt = entryOpenAt;
        this.entryCloseAt = entryCloseAt;
        this.drawAt = drawAt;
        this.gateType = gateType;
        this.gateAmount = gateAmount;
        this.gateFrom = gateFrom;
        this.minAccountAgeDays = minAccountAgeDays;
        this.weightMode = weightMode;
        this.status = "active";
    }

    /// 管理端编辑标量字段(开奖前;调用方负责校验 status=="active")。
    public void update(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            String gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
            String weightMode) {
        this.name = name;
        this.entryOpenAt = entryOpenAt;
        this.entryCloseAt = entryCloseAt;
        this.drawAt = drawAt;
        this.gateType = gateType;
        this.gateAmount = gateAmount;
        this.gateFrom = gateFrom;
        this.minAccountAgeDays = minAccountAgeDays;
        this.weightMode = weightMode;
    }

    /// 作废:任意状态均可调用(含已开奖——彩排局 id=1 先例,开奖后仍需要隐身问题期)。
    public void cancel() {
        this.status = "cancelled";
    }
}
