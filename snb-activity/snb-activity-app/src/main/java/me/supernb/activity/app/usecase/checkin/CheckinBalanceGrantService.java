package me.supernb.activity.app.usecase.checkin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinBalanceProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardCalc;
import me.supernb.activity.domain.port.checkin.BalanceGrantPort;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/// 每日返网费结算(spec 2026-07-31-checkin-daily-reward §3.1 步骤 ④)。
///
/// 🚨 **本方法必须在打卡事务提交之后调用**——它要发起外部 HTTP,绝不能与打卡写入同事务
/// (上游一抖就回滚掉用户的打卡是灾难)。
///
/// 三道判定按序:总闸 → ¥30 门槛 → 月度预算硬顶。任何一道不过都**仍然落一行
/// `balance_status='none'` 的台账**——台账要能回答「这天为什么没发钱」,空行等于查不出原因。
/// 全程异常吞掉:返网费失败绝不影响打卡本身(打卡与 NB 已在上一个事务里落定)。
@Slf4j
@Service
public class CheckinBalanceGrantService {

    private static final DateTimeFormatter NOTES_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final CheckinDailyRewardPort ledger;
    private final CheckinRechargeReadPort recharge;
    private final BalanceGrantPort grantPort;
    private final CheckinBalanceProperties balanceProps;
    private final CheckinProperties props;

    /// Spring 装配构造:BalanceGrantPort 的唯一实现 `@ConditionalOnProperty(sub2api.admin-key)`,
    /// 未配 admin-key 的环境里该 Bean 根本不存在。用 ObjectProvider 取值——它本身永远可注入,
    /// `getIfAvailable()` 无 Bean 时返回 null 而不抛,绝不让本服务拖垮整个上下文启动
    /// (照 CheckinMonthlySettlementJob 的 A-13 教训)。
    @Autowired
    public CheckinBalanceGrantService(CheckinDailyRewardPort ledger, CheckinRechargeReadPort recharge,
            ObjectProvider<BalanceGrantPort> grantPortProvider, CheckinBalanceProperties balanceProps,
            CheckinProperties props) {
        this(ledger, recharge, grantPortProvider.getIfAvailable(), balanceProps, props);
    }

    /// 全参构造(测试直接注入 mock/null grantPort,照 CheckinMonthlySettlementJob 双构造器惯例)。
    CheckinBalanceGrantService(CheckinDailyRewardPort ledger, CheckinRechargeReadPort recharge,
            BalanceGrantPort grantPort, CheckinBalanceProperties balanceProps, CheckinProperties props) {
        this.ledger = ledger;
        this.recharge = recharge;
        this.grantPort = grantPort;
        this.balanceProps = balanceProps;
        this.props = props;
    }

    /// 结算某人某天的返网费。幂等:台账 `(user_id, checkin_date)` 唯一键已有行则直接返回。
    ///
    /// @param streakDay 当日连签第几天(N),由调用方按 [CheckinDailyRewardCalc#streakDay] 算好
    /// @param now       判定历史累计充值的时间基准
    public void settle(long userId, LocalDate day, int streakDay, Instant now) {
        try {
            int nbPoints = CheckinDailyRewardCalc.nbPoints(streakDay, props.dailyNbPoints());
            String notes = "checkin-daily-" + day.format(NOTES_DAY);
            BigDecimal amount = payableAmount(userId, day, streakDay, now);

            if (amount.signum() <= 0) {
                ledger.claim(userId, day, streakDay, nbPoints, BigDecimal.ZERO, "none", notes);
                return;
            }
            Optional<Long> claimed = ledger.claim(userId, day, streakDay, nbPoints, amount, "pending", notes);
            if (claimed.isEmpty()) {
                return;   // 本日已占位(重复请求 / 补偿 job 并发),交由既有行的状态机处理
            }
            long rowId = claimed.get();
            try {
                grantPort.grant(userId, amount, notes);
                ledger.markSuccess(rowId);
            } catch (Exception e) {
                ledger.markFailed(rowId, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                log.warn("返网费发放失败,已转台账 failed 待补偿 user={} day={} amount={}", userId, day, amount, e);
            }
        } catch (Exception e) {
            // 兜底:结算链路任何意外都不得冒泡到打卡流程
            log.error("返网费结算异常(已吞,不影响打卡) user={} day={}", userId, day, e);
        }
    }

    /// 应发金额;总闸关 / grantPort 缺席 / 未过门槛 / 预算硬顶打满 一律返回 0。
    private BigDecimal payableAmount(long userId, LocalDate day, int streakDay, Instant now) {
        if (!balanceProps.enabled() || grantPort == null) {
            return BigDecimal.ZERO;
        }
        if (recharge.lifetimeRecharge(userId, now).compareTo(balanceProps.thresholdCny()) < 0) {
            return BigDecimal.ZERO;
        }
        LocalDate monthStart = day.withDayOfMonth(1);
        LocalDate monthEnd = day.withDayOfMonth(day.lengthOfMonth());
        BigDecimal spent = ledger.monthlyBalanceTotal(monthStart, monthEnd);
        if (spent.compareTo(balanceProps.monthlyCapCny()) >= 0) {
            log.error("返网费月度预算硬顶已打满,本月停发余额(NB 照发) month={} spent={} cap={}",
                    monthStart, spent, balanceProps.monthlyCapCny());
            return BigDecimal.ZERO;
        }
        return CheckinDailyRewardCalc.balanceCny(streakDay, balanceProps.perDayCny());
    }
}
