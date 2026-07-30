package me.supernb.activity.app.usecase.thursday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.app.usecase.thursday.query.ThursdayBucketQueryService;
import me.supernb.activity.domain.port.read.GateRechargeReadPort;
import me.supernb.activity.domain.port.read.ThursdayBucketReadPort;
import me.supernb.activity.domain.port.thursday.ThursdayGuessPort;
import org.junit.jupiter.api.Test;

/// 资格与桶序判定:非场次休眠 / 桶序按到账顺序 / 未达标不查领取态 / 窗口是当天整日。
class ThursdayBucketQueryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ThursdayBucketReadPort readPort = mock(ThursdayBucketReadPort.class);
    private final ThursdayGuessPort guessPort = mock(ThursdayGuessPort.class);
    private final GateRechargeReadPort rechargePort = mock(GateRechargeReadPort.class);

    /// 场次配成"今天",让被测的 LocalDate.now(ZONE) 必然命中(不改生产码去接时钟)。
    private ThursdayBucketQueryService serviceOpenToday() {
        return service(LocalDate.now(ZONE) + "=123");
    }

    private ThursdayBucketQueryService service(String sessions) {
        return new ThursdayBucketQueryService(
                new ThursdayProperties(sessions, new BigDecimal("50"), 50, 1, "opening-fk", 3, "salt", "22:00", "20:00", new BigDecimal("30")), readPort, guessPort, rechargePort);
    }

    @Test
    void nonSessionDayIsClosedAndTouchesNothing() {
        ThursdayBucketView v = service("2020-01-01=123").view(7);
        assertThat(v.open()).isFalse();
        assertThat(v.eligible()).isFalse();
        verifyNoInteractions(readPort);
    }

    @Test
    void bucketNumberFollowsRechargeArrivalOrder() {
        when(readPort.qualifiedInOrder(any(), any(), any(), anyInt())).thenReturn(List.of(11L, 22L, 33L));
        when(readPort.alreadyClaimed(anyLong(), anyLong(), any())).thenReturn(false);

        assertThat(serviceOpenToday().view(11).bucketNo()).isEqualTo(1);
        assertThat(serviceOpenToday().view(22).bucketNo()).isEqualTo(2);
        assertThat(serviceOpenToday().view(33).bucketNo()).isEqualTo(3);
    }

    @Test
    void issuedCountIsTheQualifiedHeadcount() {
        when(readPort.qualifiedInOrder(any(), any(), any(), anyInt())).thenReturn(List.of(11L, 22L, 33L));
        ThursdayBucketView v = serviceOpenToday().view(99);
        assertThat(v.issued()).isEqualTo(3);
        assertThat(v.eligible()).isFalse();
        assertThat(v.bucketNo()).isNull();
    }

    /// 不达标的人不该去查"领过没"——省一次跨库查询,也避免把未达标用户写进任何判定路径。
    @Test
    void ineligibleUserIsNotCheckedForClaim() {
        when(readPort.qualifiedInOrder(any(), any(), any(), anyInt())).thenReturn(List.of(11L));
        assertThat(serviceOpenToday().view(99).claimed()).isFalse();
        org.mockito.Mockito.verify(readPort, org.mockito.Mockito.never())
                .alreadyClaimed(anyLong(), anyLong(), any());
    }

    /// 资格窗口必须是当天 00:00(+08) 到次日 00:00(+08) —— spec §3 写的是「当天 00:00~24:00」。
    /// 早于/晚于这个窗口的充值不算数,窗口算错就是整场资格判错。
    /// 用 atLeastOnce + 取第一次调用:开奖后 view() 会再查一次(冻结窗口),不能写死只调一次。
    @Test
    void eligibilityWindowIsTheWholeLocalDay() {
        when(readPort.qualifiedInOrder(any(), any(), any(), anyInt())).thenReturn(List.of());
        serviceOpenToday().view(7);

        org.mockito.ArgumentCaptor<Instant> from = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.ArgumentCaptor<Instant> to = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(readPort, org.mockito.Mockito.atLeastOnce())
                .qualifiedInOrder(from.capture(), to.capture(), any(), anyInt());

        LocalDate today = LocalDate.now(ZONE);
        assertThat(from.getAllValues().get(0)).isEqualTo(today.atStartOfDay(ZONE).toInstant());
        assertThat(to.getAllValues().get(0)).isEqualTo(today.plusDays(1).atStartOfDay(ZONE).toInstant());
    }

    /// 🚨 开奖前一律不揭晓,而且**连查都不查**——提前泄露就等于让先充的人挑桶。
    @Test
    void beforeRevealNothingIsDisclosed() {
        LocalDate day = LocalDate.of(2026, 7, 30);
        Instant justBefore = day.atTime(21, 59, 59).atZone(ZONE).toInstant();
        assertThat(service(day + "=123").hiddenBuckets(day, justBefore)).isNull();
        verifyNoInteractions(readPort);
    }

    /// 开奖后:名单出得来,且窗口冻结在开奖时刻(不是整天)——开奖后才充的人不参与本场隐藏款。
    @Test
    void afterRevealDrawsFromTheFrozenBucketCount() {
        LocalDate day = LocalDate.of(2026, 7, 30);
        Instant justAfter = day.atTime(22, 0, 1).atZone(ZONE).toInstant();
        when(readPort.qualifiedInOrder(any(), any(), any(), anyInt())).thenReturn(List.of(1L, 2L, 3L, 4L, 5L));

        assertThat(service(day + "=123").hiddenBuckets(day, justAfter)).hasSize(3).allMatch(n -> n >= 1 && n <= 5);

        org.mockito.ArgumentCaptor<Instant> to = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(readPort).qualifiedInOrder(any(), to.capture(), any(), anyInt());
        assertThat(to.getValue()).isEqualTo(day.atTime(22, 0).atZone(ZONE).toInstant());
    }
}
