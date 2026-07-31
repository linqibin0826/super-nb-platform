package me.supernb.activity.infra.adapter.persistence;

import dev.linqibin.starter.jpa.id.SnowflakeIdGenerator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardRecord;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.infra.adapter.persistence.dao.CheckinDailyRewardJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.CheckinDailyRewardEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/// CheckinDailyRewardPort 实现:占位用原生 `INSERT ... ON CONFLICT DO NOTHING RETURNING id`
/// (同 CheckinAdapter/CheckinRewardAdapter 的写路径理由——两步"先查后插"在弱网重试/双击/
/// 补偿 job 与打卡链路并发时会撞唯一约束抛异常,单条原子语句从根源消除该窗口);
/// 状态回写走 JPA 受管实体 dirty checking(事务提交自动 flush,无需显式 save)。
@Repository
public class CheckinDailyRewardAdapter implements CheckinDailyRewardPort {

    private final CheckinDailyRewardJpaRepository rewards;
    private final JdbcTemplate jdbc;

    /// 构造:注入台账仓库与 Boot 主数据源的 JdbcTemplate。
    public CheckinDailyRewardAdapter(CheckinDailyRewardJpaRepository rewards, JdbcTemplate jdbc) {
        this.rewards = rewards;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public Optional<Long> claim(long userId, LocalDate day, int streakDay, int nbPoints,
            BigDecimal balanceCny, String balanceStatus, String notes) {
        long id = SnowflakeIdGenerator.getId();
        List<Long> inserted = jdbc.query(
                "INSERT INTO activity.checkin_daily_reward "
                        + "(id, user_id, checkin_date, streak_day, nb_points, balance_cny, balance_status, notes) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                        + "ON CONFLICT (user_id, checkin_date) DO NOTHING RETURNING id",
                (rs, i) -> rs.getLong("id"),
                id, userId, day, streakDay, nbPoints, balanceCny, balanceStatus, notes);
        return inserted.isEmpty() ? Optional.empty() : Optional.of(inserted.get(0));
    }

    @Override
    @Transactional
    public void markSuccess(long id) {
        rewards.findById(id).ifPresent(CheckinDailyRewardEntity::markSuccess);
    }

    @Override
    @Transactional
    public void markFailed(long id, String error) {
        rewards.findById(id).ifPresent(e -> e.markFailed(error));
    }

    @Override
    public Optional<CheckinDailyRewardRecord> findByUserAndDay(long userId, LocalDate day) {
        return rewards.findByUserIdAndCheckinDate(userId, day).map(CheckinDailyRewardAdapter::toRecord);
    }

    @Override
    public List<CheckinDailyRewardRecord> retryable(int maxAttempts) {
        return rewards.findRetryable(maxAttempts).stream().map(CheckinDailyRewardAdapter::toRecord).toList();
    }

    @Override
    public BigDecimal monthlyBalanceTotal(LocalDate from, LocalDate to) {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(balance_cny), 0) FROM activity.checkin_daily_reward "
                        + "WHERE checkin_date >= ? AND checkin_date <= ?",
                BigDecimal.class, from, to);
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public BigDecimal myMonthlyBalanceTotal(long userId, LocalDate from, LocalDate to) {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(balance_cny), 0) FROM activity.checkin_daily_reward "
                        + "WHERE user_id = ? AND checkin_date >= ? AND checkin_date <= ?",
                BigDecimal.class, userId, from, to);
        return v == null ? BigDecimal.ZERO : v;
    }

    private static CheckinDailyRewardRecord toRecord(CheckinDailyRewardEntity e) {
        return new CheckinDailyRewardRecord(e.getId(), e.getUserId(), e.getCheckinDate(), e.getStreakDay(),
                e.getNbPoints(), e.getBalanceCny(), e.getBalanceStatus(), e.getAttempts());
    }
}
