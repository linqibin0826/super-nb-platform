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
import me.supernb.activity.domain.port.read.ThursdayBucketReadPort;
import org.junit.jupiter.api.Test;

/// 资格与桶序判定:非场次休眠 / 桶序按到账顺序 / 未达标不查领取态 / 窗口是当天整日。
class ThursdayBucketQueryServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ThursdayBucketReadPort readPort = mock(ThursdayBucketReadPort.class);

    /// 场次配成"今天",让被测的 LocalDate.now(ZONE) 必然命中(不改生产码去接时钟)。
    private ThursdayBucketQueryService serviceOpenToday() {
        return service(LocalDate.now(ZONE) + "=123");
    }

    private ThursdayBucketQueryService service(String sessions) {
        return new ThursdayBucketQueryService(
                new ThursdayProperties(sessions, new BigDecimal("50"), 50, 1, "opening-fk"), readPort);
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

    /// 窗口必须是当天 00:00(+08) 到次日 00:00(+08) —— spec §3 写的是「当天 00:00~24:00」。
    /// 早于/晚于这个窗口的充值不算数,窗口算错就是整场资格判错。
    @Test
    void windowIsTheWholeLocalDay() {
        when(readPort.qualifiedInOrder(any(), any(), any(), anyInt())).thenReturn(List.of());
        serviceOpenToday().view(7);

        org.mockito.ArgumentCaptor<Instant> from = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.ArgumentCaptor<Instant> to = org.mockito.ArgumentCaptor.forClass(Instant.class);
        org.mockito.Mockito.verify(readPort)
                .qualifiedInOrder(from.capture(), to.capture(), any(), anyInt());

        LocalDate today = LocalDate.now(ZONE);
        assertThat(from.getValue()).isEqualTo(today.atStartOfDay(ZONE).toInstant());
        assertThat(to.getValue()).isEqualTo(today.plusDays(1).atStartOfDay(ZONE).toInstant());
    }
}
