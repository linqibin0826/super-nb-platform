package me.supernb.activity.app.usecase.checkin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import me.supernb.activity.app.usecase.checkin.config.CheckinBalanceProperties;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardCalc;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardRecord;
import me.supernb.activity.domain.port.checkin.BalanceGrantPort;
import me.supernb.activity.domain.port.checkin.CheckinDailyRewardPort;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/// 返网费补偿 job(每 10 分钟)。职责有二,一个 job 同时解决重试与补录:
///
/// ① 重发 pending / failed(attempts<3) 的台账行——覆盖打卡当场上游抖动、超时等;
/// ② 补录「今天 / 昨天有 checkin_record 但台账缺行」的用户——**这条自然覆盖了新功能
///    上线前当天已经打过卡的人,不需要任何一次性补扫脚本**。
///
/// 幂等全靠台账 `(user_id, checkin_date)` 唯一键,重复跑无害。
@Slf4j
@Service
public class CheckinBalanceRetryJob {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter NOTES_DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_ATTEMPTS = 3;

    private final CheckinDailyRewardPort ledger;
    private final CheckinPort checkinPort;
    private final BalanceGrantPort grantPort;
    private final CheckinBalanceGrantService settleService;
    private final CheckinBalanceProperties props;

    /// Spring 装配构造:grantPort 走 ObjectProvider,无 admin-key 环境该 Bean 不存在(同 A-13 教训)。
    @Autowired
    public CheckinBalanceRetryJob(CheckinDailyRewardPort ledger, CheckinPort checkinPort,
            ObjectProvider<BalanceGrantPort> grantPortProvider, CheckinBalanceGrantService settleService,
            CheckinBalanceProperties props) {
        this(ledger, checkinPort, grantPortProvider.getIfAvailable(), settleService, props);
    }

    /// 全参构造(测试直接注入 mock/null grantPort,照家族双构造器惯例)。
    CheckinBalanceRetryJob(CheckinDailyRewardPort ledger, CheckinPort checkinPort, BalanceGrantPort grantPort,
            CheckinBalanceGrantService settleService, CheckinBalanceProperties props) {
        this.ledger = ledger;
        this.checkinPort = checkinPort;
        this.grantPort = grantPort;
        this.settleService = settleService;
        this.props = props;
    }

    /// 每 10 分钟跑一次(Asia/Shanghai)。
    @Scheduled(cron = "0 */10 * * * *", zone = "Asia/Shanghai")
    public void run() {
        if (!props.enabled() || grantPort == null) {
            return;
        }
        try {
            retryPendingAndFailed();
        } catch (Exception e) {
            log.error("返网费补偿——重试阶段异常", e);
        }
        try {
            LocalDate today = LocalDate.now(ZONE);
            backfillMissingLedger(today);
            backfillMissingLedger(today.minusDays(1));
        } catch (Exception e) {
            log.error("返网费补偿——补录阶段异常", e);
        }
    }

    /// 重发 pending / failed(attempts<3)。
    private void retryPendingAndFailed() {
        List<CheckinDailyRewardRecord> rows = ledger.retryable(MAX_ATTEMPTS);
        for (CheckinDailyRewardRecord r : rows) {
            if (r.balanceCny() == null || r.balanceCny().signum() <= 0) {
                continue;   // 防御:金额为 0 的行不该是 pending/failed,跳过不发
            }
            String notes = "checkin-daily-" + r.checkinDate().format(NOTES_DAY);
            try {
                grantPort.grant(r.userId(), r.balanceCny(), notes);
                ledger.markSuccess(r.id());
            } catch (Exception e) {
                ledger.markFailed(r.id(), e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                log.warn("返网费重试仍失败 user={} day={} attempts={}", r.userId(), r.checkinDate(), r.attempts(), e);
            }
        }
    }

    /// 补录某天已打卡但台账缺行的用户(上线前已签到者由此兜住)。
    private void backfillMissingLedger(LocalDate day) {
        for (long userId : checkinPort.userIdsCheckedInOn(day)) {
            if (ledger.findByUserAndDay(userId, day).isPresent()) {
                continue;
            }
            int streakDay = CheckinDailyRewardCalc.streakDay(
                    checkinPort.datesInRange(userId, day.withDayOfMonth(1), day), day);
            settleService.settle(userId, day, streakDay, Instant.now());
            log.info("返网费补录 user={} day={} streakDay={}", userId, day, streakDay);
        }
    }
}
