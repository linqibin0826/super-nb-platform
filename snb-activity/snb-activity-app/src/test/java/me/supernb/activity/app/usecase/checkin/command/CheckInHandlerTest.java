package me.supernb.activity.app.usecase.checkin.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.domain.exception.CheckinAlreadyDoneException;
import me.supernb.activity.domain.exception.CheckinTooYoungException;
import me.supernb.activity.domain.model.checkin.CheckInResult;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/// 签到 Handler:账龄门槛过了才委托 CheckinPort;查无注册记录/不足 24 小时一律 403;
/// 今日已打过卡(幂等回放)一律 409;首次成功则回填累计天数与连续天数。
class CheckInHandlerTest {

    private final AccountRegistrationReadPort registration = mock(AccountRegistrationReadPort.class);
    private final CheckinPort checkinPort = mock(CheckinPort.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CheckInHandler handler = new CheckInHandler(registration, checkinPort, new CheckinProperties("2020-01-01", 3), events);

    @Test
    void unknownRegistrationRejectedWith403() {
        when(registration.registeredAt(42)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new CheckInCommand(42)))
                .isInstanceOf(CheckinTooYoungException.class);
    }

    @Test
    void accountYoungerThan24HoursRejectedWith403() {
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600)));
        assertThatThrownBy(() -> handler.handle(new CheckInCommand(42)))
                .isInstanceOf(CheckinTooYoungException.class);
    }

    @Test
    void alreadyCheckedInTodayRejectedWith409() {
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        when(checkinPort.checkIn(eq(42L), any(), any(), anyInt()))
                .thenReturn(new CheckinOutcome(false, LocalDate.now(), Instant.now().minusSeconds(3600)));
        assertThatThrownBy(() -> handler.handle(new CheckInCommand(42)))
                .isInstanceOf(CheckinAlreadyDoneException.class);
        verify(events, never()).publishEvent(any()); // 幂等回放不发事件
    }

    @Test
    void firstCheckInTodayReturnsCumulativeAndStreak() {
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        when(checkinPort.checkIn(eq(42L), any(), any(), anyInt()))
                .thenReturn(new CheckinOutcome(true, today, Instant.now()));
        when(checkinPort.totalCheckins(42)).thenReturn(13);
        when(checkinPort.datesInRange(eq(42L), any(), any())).thenReturn(List.of(today, today.minusDays(1)));

        CheckInResult result = handler.handle(new CheckInCommand(42));

        assertThat(result.checkinDate()).isEqualTo(today);
        assertThat(result.cumulativeDays()).isEqualTo(13);
        assertThat(result.streakCurrent()).isEqualTo(2);
        verify(events).publishEvent(new UserCheckedInEvent(42, today)); // 首次打卡发布事实事件(成就侧即时解锁)
    }
}
