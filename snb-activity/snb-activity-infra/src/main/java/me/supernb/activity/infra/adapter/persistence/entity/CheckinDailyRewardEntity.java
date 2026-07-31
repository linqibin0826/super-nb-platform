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

/// 每日返网费台账 JPA 实体,映射 `activity.checkin_daily_reward`。
///
/// `(user_id, checkin_date)` 唯一键是「一天只发一次」的并发仲裁真源;
/// balance_status 状态机 none | pending → success | failed。
@Entity
@Table(name = "checkin_daily_reward", schema = "activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckinDailyRewardEntity extends BaseJpaEntity {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "checkin_date")
    private LocalDate checkinDate;

    @Column(name = "streak_day")
    private int streakDay;

    @Column(name = "nb_points")
    private int nbPoints;

    @Column(name = "balance_cny")
    private BigDecimal balanceCny;

    @Column(name = "balance_status")
    private String balanceStatus;

    @Column(name = "attempts")
    private int attempts;

    @Column(name = "notes")
    private String notes;

    @Column(name = "last_error")
    private String lastError;

    /// 标记返网费发放成功。
    public void markSuccess() {
        this.balanceStatus = "success";
    }

    /// 标记返网费发放失败:累加尝试次数,记录错误信息。
    public void markFailed(String error) {
        this.balanceStatus = "failed";
        this.attempts++;
        this.lastError = error;
    }
}
