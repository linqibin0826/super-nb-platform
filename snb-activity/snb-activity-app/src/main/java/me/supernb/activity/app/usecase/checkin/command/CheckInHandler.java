package me.supernb.activity.app.usecase.checkin.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import me.supernb.activity.domain.exception.CheckinAlreadyDoneException;
import me.supernb.activity.domain.exception.CheckinTooYoungException;
import me.supernb.activity.domain.model.checkin.CheckInResult;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;
import me.supernb.activity.domain.model.checkin.CheckinStreak;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/// 签到编排:账龄 ≥24 小时才放行(spec §3.1,呼应 07-10 注册即送事故教训,403)→
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

    /// 构造:注入账龄读端口、签到端口与签到配置(每日进账单价)。
    public CheckInHandler(AccountRegistrationReadPort registration, CheckinPort checkinPort,
            CheckinProperties props, ApplicationEventPublisher events) {
        this.registration = registration;
        this.checkinPort = checkinPort;
        this.props = props;
        this.events = events;
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
        CheckinOutcome outcome = checkinPort.checkIn(command.userId(), today, now, props.dailyNbPoints());
        if (!outcome.firstCheckinToday()) {
            throw new CheckinAlreadyDoneException();
        }
        // 首次打卡成功(事务已在 CheckinAdapter 提交)→ 发布事实事件,成就侧同步监听即时补写指标
        // 并判定解锁(开机自检等打卡当场亮);checkin 不依赖成就侧,成就同步失败也不影响本次打卡。
        events.publishEvent(new UserCheckedInEvent(command.userId(), today));
        int cumulativeDays = checkinPort.totalCheckins(command.userId());
        var recentDates = checkinPort.datesInRange(command.userId(),
                today.minusDays(STREAK_LOOKBACK_DAYS), today);
        int streakCurrent = CheckinStreak.current(recentDates, today);
        return new CheckInResult(today, cumulativeDays, streakCurrent);
    }
}
