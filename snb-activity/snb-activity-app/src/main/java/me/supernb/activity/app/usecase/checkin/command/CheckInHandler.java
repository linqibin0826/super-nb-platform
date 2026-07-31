package me.supernb.activity.app.usecase.checkin.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.supernb.activity.domain.exception.CheckinAlreadyDoneException;
import me.supernb.activity.domain.exception.CheckinRechargeRequiredException;
import me.supernb.activity.domain.exception.CheckinTooYoungException;
import me.supernb.activity.domain.model.checkin.CheckInResult;
import me.supernb.activity.domain.model.checkin.CheckinDailyRewardCalc;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;
import me.supernb.activity.domain.model.checkin.CheckinStreak;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.app.usecase.checkin.CheckinBalanceGrantService;
import me.supernb.activity.app.usecase.checkin.CheckinEntryGateChecker;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/// 签到编排:账龄 ≥24 小时才放行(spec §3.1,呼应 07-10 注册即送事故教训,403)→
/// 准入闸(spec §12:近 30 天真实充值 ≥¥30,403——状态接口只是展示,这里才是真拦截)→
/// 委托 CheckinPort 完成幂等写入;今日已打过卡则 409(前端契约,不是静默成功)→
/// 首次成功则回填累计天数/连续天数供响应体使用。自然日显式用 Asia/Shanghai(红二1 红线),
/// 不依赖容器/DB 默认时区。事务边界在 infra(CheckinAdapter),本层无事务注解(家族约定)。
@Service
public class CheckInHandler implements CommandHandler<CheckInCommand, CheckInResult> {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Duration MIN_ACCOUNT_AGE = Duration.ofHours(24);
    private static final int STREAK_LOOKBACK_DAYS = 60;

    private final AccountRegistrationReadPort registration;
    private final CheckinPort checkinPort;
    private final CheckinProperties props;
    private final ApplicationEventPublisher events;
    private final CheckinBalanceGrantService balanceGrant;
    private final CheckinEntryGateChecker entryGate;

    /// 构造:注入账龄读端口、签到端口、签到配置(NB 单价)、事件发布器、返网费结算服务
    /// 与准入闸判定器(与状态查询共用同一真源)。
    public CheckInHandler(AccountRegistrationReadPort registration, CheckinPort checkinPort,
            CheckinProperties props, ApplicationEventPublisher events,
            CheckinBalanceGrantService balanceGrant, CheckinEntryGateChecker entryGate) {
        this.registration = registration;
        this.checkinPort = checkinPort;
        this.props = props;
        this.events = events;
        this.balanceGrant = balanceGrant;
        this.entryGate = entryGate;
    }

    @Override
    public CheckInResult handle(CheckInCommand command) {
        Instant now = Instant.now();
        Instant registeredAt = registration.registeredAt(command.userId())
                .orElseThrow(CheckinTooYoungException::new);
        if (registeredAt.isAfter(now.minus(MIN_ACCOUNT_AGE))) {
            throw new CheckinTooYoungException();
        }
        LocalDate today = LocalDate.now(ZONE);
        if (entryGate.enabled() && !entryGate.check(command.userId(), today).eligible()) {
            throw new CheckinRechargeRequiredException(entryGate.lockedMessage());
        }
        // 连签第 N 天(2026-07-31 起 NB 随 N 递增):只取本月日期喂给纯计算,
        // 「月初清零」由集合里天然没有上月日期保证,无需额外边界判断。
        LocalDate monthStart = today.withDayOfMonth(1);
        int streakDay = CheckinDailyRewardCalc.streakDay(
                checkinPort.datesInRange(command.userId(), monthStart, today), today);
        int nbPoints = CheckinDailyRewardCalc.nbPoints(streakDay, props.dailyNbPoints());

        CheckinOutcome outcome = checkinPort.checkIn(command.userId(), today, now, nbPoints);
        if (!outcome.firstCheckinToday()) {
            throw new CheckinAlreadyDoneException();
        }
        // 首次打卡成功(事务已在 CheckinAdapter 提交)→ 发布事实事件,成就侧同步监听即时补写指标
        // 并判定解锁(开机自检等打卡当场亮);checkin 不依赖成就侧,成就同步失败也不影响本次打卡。
        events.publishEvent(new UserCheckedInEvent(command.userId(), today));
        // 🚨 返网费必须在打卡事务提交之后:它要发外部 HTTP,同事务会让上游一抖就回滚掉
        // 用户的打卡。settle 内部吞掉一切异常,失败只落台账 failed 交补偿 job 重试。
        balanceGrant.settle(command.userId(), today, streakDay, now);

        int cumulativeDays = checkinPort.totalCheckins(command.userId());
        var recentDates = checkinPort.datesInRange(command.userId(),
                today.minusDays(STREAK_LOOKBACK_DAYS), today);
        int streakCurrent = CheckinStreak.current(recentDates, today);
        return new CheckInResult(today, cumulativeDays, streakCurrent);
    }
}
