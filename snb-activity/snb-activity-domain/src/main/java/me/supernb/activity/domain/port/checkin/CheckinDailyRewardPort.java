package me.supernb.activity.domain.port.checkin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardRecord;

/// 每日返网费台账端口(活动库):读写合一,照 CheckinRewardPort/CheckinPort 惯例。
/// `(user_id, checkin_date)` 唯一键是「一天只发一次」的幂等真源——上游 admin API
/// 自带的幂等键 TTL 仅 2 小时,不能当长期依赖。
public interface CheckinDailyRewardPort {

    /// 占位当日台账行:已存在则不新建(返回 empty,幂等,不因"想改档"而二次插入),
    /// 否则原子插入返回其 id。
    Optional<Long> claim(long userId, LocalDate day, int streakDay, int nbPoints,
            BigDecimal balanceCny, String balanceStatus, String notes);

    /// 标记返网费发放成功。
    void markSuccess(long id);

    /// 标记返网费发放失败(累加尝试次数,记录错误信息)。
    void markFailed(long id, String error);

    /// 取某人某天的台账行。
    Optional<CheckinDailyRewardRecord> findByUserAndDay(long userId, LocalDate day);

    /// 可重试的行:balance_status ∈ (pending, failed) 且 attempts < maxAttempts。
    List<CheckinDailyRewardRecord> retryable(int maxAttempts);

    /// [from, to] 闭区间内全站已发返网费合计(月度预算硬顶判定用);无行返回 0。
    BigDecimal monthlyBalanceTotal(LocalDate from, LocalDate to);

    /// [from, to] 闭区间内某人已发返网费合计(页面「本月已返」);无行返回 0。
    BigDecimal myMonthlyBalanceTotal(long userId, LocalDate from, LocalDate to);
}
