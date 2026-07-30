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

/// 猜桶竞猜的一次提交,映射 `activity.thursday_guess`。
///
/// `(session_date, user_id)` 唯一键是「一人一场一猜」的并发仲裁真源,
/// 也让「封猜前也不能改猜」成为库层面的事实——猜完就定死,和群接龙「猜完不改」一致。
@Entity
@Table(name = "thursday_guess", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThursdayGuessEntity extends BaseJpaEntity {

    /// 场次自然日(Asia/Shanghai 口径由调用方换算)。
    @Column(name = "session_date")
    private LocalDate sessionDate;

    /// 猜的人(sub2api user id)。
    @Column(name = "user_id")
    private Long userId;

    /// 猜的份数。
    @Column(name = "guess")
    private int guess;

    /// 构造:一次提交,雪花 id 显式预分配。
    public ThursdayGuessEntity(LocalDate sessionDate, long userId, int guess) {
        setId(SnowflakeIdGenerator.getId());
        this.sessionDate = sessionDate;
        this.userId = userId;
        this.guess = guess;
    }
}
