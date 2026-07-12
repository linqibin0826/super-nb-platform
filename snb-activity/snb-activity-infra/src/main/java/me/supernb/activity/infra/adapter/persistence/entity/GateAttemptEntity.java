package me.supernb.activity.infra.adapter.persistence.entity;

import dev.linqibin.starter.jpa.entity.BaseJpaEntity;
import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/// 金票每日抽签记录 JPA 实体,映射 `activity.gate_attempt`。
///
/// `(user_id, attempt_date)` 唯一键是「每人每日一次」的并发仲裁真源;中/未中都落库(审计)。
@Entity
@Table(name = "gate_attempt", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GateAttemptEntity extends BaseJpaEntity {

    /// 抽签用户(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 抽签自然日(Asia/Shanghai 口径由调用方换算)。
    @Column(name = "attempt_date")
    private LocalDate attemptDate;

    /// 是否中签。
    @Column(name = "won")
    private boolean won;

    /// 中签票 id(未中为空)。
    @Column(name = "ticket_id")
    private Long ticketId;

    /// 构造:当日一笔抽签记录,雪花 id 显式预分配。
    public GateAttemptEntity(long userId, LocalDate attemptDate, boolean won, Long ticketId) {
        setId(SnowflakeIdGenerator.getId());
        this.userId = userId;
        this.attemptDate = attemptDate;
        this.won = won;
        this.ticketId = ticketId;
    }
}
