package me.supernb.activity.app.usecase.checkin.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.app.usecase.checkin.CheckinBalanceGrantService;
import me.supernb.activity.app.usecase.checkin.CheckinEntryGateChecker;
import me.supernb.activity.app.usecase.checkin.config.CheckinEntryGateProperties;
import me.supernb.activity.app.usecase.checkin.config.CheckinProperties;
import me.supernb.activity.domain.exception.CheckinAlreadyDoneException;
import me.supernb.activity.domain.exception.CheckinBalanceNegativeException;
import me.supernb.activity.domain.exception.CheckinRechargeRequiredException;
import me.supernb.activity.domain.exception.CheckinTooYoungException;
import me.supernb.activity.domain.model.checkin.CheckInResult;
import me.supernb.activity.domain.model.checkin.CheckinOutcome;
import me.supernb.activity.domain.port.checkin.CheckinPort;
import me.supernb.activity.domain.port.read.AccountRegistrationReadPort;
import me.supernb.activity.domain.port.read.CheckinRechargeReadPort;
import me.supernb.activity.domain.port.read.UserBalanceReadPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/// 签到 Handler:账龄门槛过了才委托 CheckinPort;查无注册记录/不足 24 小时一律 403;
/// 准入闸(spec §12)未过一律 403 且绝不落打卡;今日已打过卡(幂等回放)一律 409;
/// 首次成功则回填累计天数与连续天数。
class CheckInHandlerTest {

    private final AccountRegistrationReadPort registration = mock(AccountRegistrationReadPort.class);
    private final CheckinPort checkinPort = mock(CheckinPort.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CheckinBalanceGrantService balanceGrant = mock(CheckinBalanceGrantService.class);
    private final CheckinRechargeReadPort rechargePort = mock(CheckinRechargeReadPort.class);
    private final UserBalanceReadPort balancePort = mock(UserBalanceReadPort.class);
    private final CheckInHandler handler = handlerWithGate(new CheckinEntryGateProperties(false, 30, "30"));

    /// 余额读端口默认给 0(未 stub 的 BigDecimal 返回 null,会在 signum 处 NPE);
    /// 负余额用例自行覆盖。
    @BeforeEach
    void stubBalanceDefault() {
        when(balancePort.balance(anyLong())).thenReturn(BigDecimal.ZERO);
    }

    /// 组装被测 Handler(准入闸判定器用真实现+mock 充值端口;默认闸关,既有用例旧行为不变)。
    private CheckInHandler handlerWithGate(CheckinEntryGateProperties gateProps) {
        return new CheckInHandler(registration, checkinPort, new CheckinProperties("2020-01-01", 3), events,
                balanceGrant, new CheckinEntryGateChecker(rechargePort, gateProps), balancePort);
    }

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
    void entryGateShortfallRejectedWith403BeforeAnyWrite() {
        // 近 30 天没充够 ¥30:403,一笔都不写——状态接口只是展示,这里才是真拦截
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        CheckInHandler gated = handlerWithGate(new CheckinEntryGateProperties(true, 30, "30"));
        // rechargeEvents 未 stub → Mockito 默认空列表 = 窗口内零充值

        assertThatThrownBy(() -> gated.handle(new CheckInCommand(42)))
                .isInstanceOf(CheckinRechargeRequiredException.class)
                .hasMessage("近 30 天充值满 ¥30 才能上机签到");
        verify(checkinPort, never()).checkIn(anyLong(), any(), any(), anyInt());
        verify(balanceGrant, never()).settle(anyLong(), any(), anyInt(), any());
    }

    @Test
    void entryGatePassedProceedsToCheckIn() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        when(rechargePort.rechargeEvents(eq(42L), any(), any())).thenReturn(List.of(
                new CheckinRechargeReadPort.RechargeEvent(Instant.now().minusSeconds(3600), new java.math.BigDecimal("30"))));
        when(checkinPort.checkIn(eq(42L), any(), any(), anyInt()))
                .thenReturn(new CheckinOutcome(true, today, Instant.now()));
        when(checkinPort.totalCheckins(42)).thenReturn(1);
        when(checkinPort.datesInRange(eq(42L), any(), any())).thenReturn(List.of(today));

        CheckInHandler gated = handlerWithGate(new CheckinEntryGateProperties(true, 30, "30"));
        CheckInResult result = gated.handle(new CheckInCommand(42));

        assertThat(result.checkinDate()).isEqualTo(today);
    }

    @Test
    void negativeBalanceRejectedWith403BeforeAnyWrite() {
        // 余额欠费(计费透支,2026-08-17 站长拍板):403 拦在打卡之前——不落卡、不发 NB、不结返网费
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        when(balancePort.balance(42L)).thenReturn(new BigDecimal("-0.34"));

        assertThatThrownBy(() -> handler.handle(new CheckInCommand(42)))
                .isInstanceOf(CheckinBalanceNegativeException.class)
                .hasMessageContaining("0.34");
        verify(checkinPort, never()).checkIn(anyLong(), any(), any(), anyInt());
        verify(balanceGrant, never()).settle(anyLong(), any(), anyInt(), any());
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

    @Test
    void nbPointsScaleWithStreakDayAndBalanceSettledAfterCommit() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        LocalDate monthStart = today.withDayOfMonth(1);
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        // 本月连签到昨天为止 6 天 → 今天是第 7 天(月内不越界:只取 today 之后的 6 天)
        List<LocalDate> month = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            LocalDate d = today.minusDays(i);
            if (!d.isBefore(monthStart)) {
                month.add(d);
            }
        }
        int expectedStreak = month.size() + 1;
        when(checkinPort.datesInRange(eq(42L), eq(monthStart), eq(today))).thenReturn(month);
        when(checkinPort.checkIn(eq(42L), any(), any(), anyInt()))
                .thenReturn(new CheckinOutcome(true, today, Instant.now()));
        when(checkinPort.totalCheckins(42)).thenReturn(expectedStreak);

        handler.handle(new CheckInCommand(42));

        verify(checkinPort).checkIn(eq(42L), eq(today), any(), eq(3 * expectedStreak));
        verify(balanceGrant).settle(eq(42L), eq(today), eq(expectedStreak), any());
    }

    @Test
    void balanceNotSettledOnIdempotentReplay() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Shanghai"));
        when(registration.registeredAt(42)).thenReturn(Optional.of(Instant.now().minusSeconds(3600 * 48)));
        when(checkinPort.datesInRange(anyLong(), any(), any())).thenReturn(List.of());
        when(checkinPort.checkIn(eq(42L), any(), any(), anyInt()))
                .thenReturn(new CheckinOutcome(false, today, Instant.now()));

        assertThatThrownBy(() -> handler.handle(new CheckInCommand(42)))
                .isInstanceOf(CheckinAlreadyDoneException.class);
        verify(balanceGrant, never()).settle(anyLong(), any(), anyInt(), any());
    }
}
