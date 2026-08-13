package me.supernb.activity.app.usecase.school.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import me.supernb.activity.app.usecase.school.SchoolStatusView;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.junit.jupiter.api.Test;

/// 状态查询:窗口分支(休眠/宽限/收官)、首充定档取最高档、领取态只认领取表、
/// 邀请计数封顶 10 与 KFC 解锁。
class SchoolStatusQueryServiceTest {

    private static final Instant START = Instant.parse("2026-08-13T04:00:00Z");
    private static final Instant END = Instant.parse("2026-08-31T16:00:00Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-08-20T00:00:00Z");

    private final SchoolReadPort read = mock(SchoolReadPort.class);
    private final SchoolClaimPort claims = mock(SchoolClaimPort.class);

    private SchoolStatusQueryService svc() {
        SchoolSeasonProperties props = new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 3, "school-season");
        return new SchoolStatusQueryService(props, read, claims);
    }

    @Test
    void dormantConfigIsClosed() {
        SchoolSeasonProperties dormant = new SchoolSeasonProperties(
                "", "", "", 0, 0, 0, 0, 0, 0, 3, "school-season");
        SchoolStatusQueryService svc = new SchoolStatusQueryService(dormant, read, claims);
        assertThat(svc.view(1L, IN_WINDOW).open()).isFalse();
    }

    @Test
    void afterClaimDeadlineIsClosed() {
        assertThat(svc().view(1L, Instant.parse("2026-09-03T00:00:00Z")).open()).isFalse();
    }

    @Test
    void beforeStartIsClosed() {
        assertThat(svc().view(1L, Instant.parse("2026-08-01T00:00:00Z")).open()).isFalse();
    }

    @Test
    void graceWindowStillOpenButEligibilityBoundToEnd() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("50.00"), IN_WINDOW)));
        when(read.qualifiedInviteeCount(1L, START, END, new BigDecimal("30"))).thenReturn(0);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        SchoolStatusView v = svc().view(1L, Instant.parse("2026-09-01T16:00:00Z"));
        assertThat(v.open()).isTrue();
        assertThat(v.firstCharge().status()).isEqualTo("claimable");
        // 资格查询边界钉死 end,不随 now 后移
        verify(read).qualifiedInviteeCount(1L, START, END, new BigDecimal("30"));
    }

    @Test
    void firstChargeTierPicksHighest() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("100.00"), IN_WINDOW)));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        assertThat(svc().view(1L, IN_WINDOW).firstCharge().tierCard()).isEqualTo(200);
    }

    @Test
    void firstChargeBeforeWindowGetsNothing() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("500.00"), Instant.parse("2026-07-01T00:00:00Z"))));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        SchoolStatusView.FirstChargeBlock fc = svc().view(1L, IN_WINDOW).firstCharge();
        assertThat(fc.charged()).isTrue();
        assertThat(fc.inWindow()).isFalse();
        assertThat(fc.tierCard()).isZero();
        assertThat(fc.status()).isEqualTo("none");
    }

    @Test
    void firstChargeBelow30GetsNothing() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("20.00"), IN_WINDOW)));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        assertThat(svc().view(1L, IN_WINDOW).firstCharge().tierCard()).isZero();
    }

    @Test
    void claimedStateComesFromClaimTableOnly() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("30.00"), IN_WINDOW)));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50)).thenReturn(Optional.of(
                new SchoolClaimRecord(9L, 1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50, 129L,
                        SchoolClaimRecord.STATUS_SUCCESS, 1, null)));
        assertThat(svc().view(1L, IN_WINDOW).firstCharge().status()).isEqualTo("claimed");
    }

    @Test
    void inviteCountCappedAt10AndUnlocksKfc() {
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(23);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        SchoolStatusView.InviteBlock inv = svc().view(1L, IN_WINDOW).invite();
        assertThat(inv.count()).isEqualTo(10);
        assertThat(inv.kfcUnlocked()).isTrue();
        assertThat(inv.milestones()).extracting(SchoolStatusView.Milestone::unlocked)
                .containsExactly(true, true, true);
    }

    @Test
    void milestoneCardAmountsAndPartialUnlock() {
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(4);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        var ms = svc().view(1L, IN_WINDOW).invite().milestones();
        assertThat(ms).extracting(SchoolStatusView.Milestone::tier).containsExactly(1, 3, 6);
        assertThat(ms).extracting(SchoolStatusView.Milestone::cardAmount).containsExactly(50, 100, 200);
        assertThat(ms).extracting(SchoolStatusView.Milestone::unlocked).containsExactly(true, true, false);
        assertThat(ms.get(2).status()).isEqualTo("none");
    }

    @Test
    void endsAtLabelIsShanghaiRendered() {
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        // 2026-08-31T16:00Z = 北京 09-01 00:00
        assertThat(svc().view(1L, IN_WINDOW).endsAtLabel()).isEqualTo("9月1日 00:00");
    }
}
