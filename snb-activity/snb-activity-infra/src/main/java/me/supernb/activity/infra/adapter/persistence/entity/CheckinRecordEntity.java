package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 签到记录 JPA 实体,映射 `activity.checkin_record`。
///
/// `(user_id, checkin_date)` 唯一键是「每人每日一次」的并发仲裁真源,照抄 gate_attempt 模板。
/// 生产写路径经 [me.supernb.activity.infra.adapter.persistence.CheckinAdapter] 的原生
/// `INSERT ... ON CONFLICT DO NOTHING RETURNING` 语句写入(不经本类构造器),
/// 本类主要供读查询与测试直接落库使用。
@Entity
@Table(name = "checkin_record", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckinRecordEntity extends BaseJpaEntity {

    /// 签到用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 签到自然日(Asia/Shanghai 口径由调用方换算)。
    @Column(name = "checkin_date")
    private LocalDate checkinDate;

    /// 精确签到时刻(深化稿决策⑥,零点信使等隐藏成就依赖此列)。
    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    /// 构造:一天一条签到记录,雪花 id 显式预分配。
    public CheckinRecordEntity(long userId, LocalDate checkinDate, Instant checkedInAt) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        this.checkinDate = checkinDate;
        this.checkedInAt = checkedInAt;
    }
}
