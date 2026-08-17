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
import me.supernb.activity.domain.model.school.SchoolCardRecord;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import me.supernb.activity.domain.port.school.SchoolCardPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.junit.jupiter.api.Test;

/// 状态查询:窗口分支(休眠/宽限/收官)、首充定档取最高档、领取态只认领取表、
/// 邀请卡 v2(已领档/应得档/重置银行推导)与 KFC 20 人解锁。
class SchoolStatusQueryServiceTest {

    private static final Instant START = Instant.parse("2026-08-13T04:00:00Z");
    private static final Instant END = Instant.parse("2026-08-31T16:00:00Z");
    private static final Instant IN_WINDOW = Instant.parse("2026-08-20T00:00:00Z");

    private final SchoolReadPort read = mock(SchoolReadPort.class);
    private final SchoolClaimPort claims = mock(SchoolClaimPort.class);
    private final SchoolCardPort cards = mock(SchoolCardPort.class);

    private SchoolStatusQueryService svc() {
        SchoolSeasonProperties props = new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 135L, "school-season");
        return new SchoolStatusQueryService(props, read, claims, cards);
    }

    @Test
    void dormantConfigIsClosed() {
        SchoolSeasonProperties dormant = new SchoolSeasonProperties(
                "", "", "", 0, 0, 0, 0, 0, 0, 0, "school-season");
        SchoolStatusQueryService svc = new SchoolStatusQueryService(dormant, read, claims, cards);
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
        when(read.qualifiedInviteeCount(1L, START, END, new BigDecimal("50"))).thenReturn(0);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(cards.find(anyLong())).thenReturn(Optional.empty());
        SchoolStatusView v = svc().view(1L, Instant.parse("2026-09-01T16:00:00Z"));
        assertThat(v.open()).isTrue();
        assertThat(v.firstCharge().status()).isEqualTo("claimable");
        // 资格查询边界钉死 end,不随 now 后移
        verify(read).qualifiedInviteeCount(1L, START, END, new BigDecimal("50"));
    }

    @Test
    void firstChargeTierPicksHighest() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("200.00"), IN_WINDOW)));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        when(claims.find(anyLong(), anyString(), anyInt())).thenReturn(Optional.empty());
        when(cards.find(anyLong())).thenReturn(Optional.empty());
        assertThat(svc().view(1L, IN_WINDOW).firstCharge().tierCard()).isEqualTo(200);
    }

    @Test
    void firstChargeOutOfWindowHasNoTier() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("100.00"),
                        Instant.parse("2026-08-01T00:00:00Z"))));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        when(cards.find(anyLong())).thenReturn(Optional.empty());
        SchoolStatusView v = svc().view(1L, IN_WINDOW);
        assertThat(v.firstCharge().charged()).isTrue();
        assertThat(v.firstCharge().inWindow()).isFalse();
        assertThat(v.firstCharge().tierCard()).isZero();
    }

    @Test
    void firstChargeClaimStatusFollowsClaimTable() {
        when(read.firstCharge(1L)).thenReturn(Optional.of(
                new SchoolReadPort.FirstCharge(new BigDecimal("50.00"), IN_WINDOW)));
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(0);
        when(cards.find(anyLong())).thenReturn(Optional.empty());
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50)).thenReturn(Optional.of(
                new SchoolClaimRecord(9L, 1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50, 129L,
                        SchoolClaimRecord.STATUS_SUCCESS, 1, null)));
        assertThat(svc().view(1L, IN_WINDOW).firstCharge().status()).isEqualTo("claimed");
    }

    @Test
    void noCardYetShowsClaimableGoAtOneInvitee() {
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(1);
        when(cards.find(1L)).thenReturn(Optional.empty());
        SchoolStatusView.CardBlock c = svc().view(1L, IN_WINDOW).invite().card();
        assertThat(c.tier()).isZero();
        assertThat(c.claimableTier()).isEqualTo(1);
        assertThat(c.claimableName()).isEqualTo("Go");
        assertThat(c.claimableCard()).isEqualTo(30);
        assertThat(c.resetsAvailable()).isZero();   // 第 1 人是开卡节点,无重置
    }

    @Test
    void cardTierAndResetBankDerived() {
        // 7 人已领 Plus:earned=7-2(节点1,5)=5,已用 2 → 可用 3;应得档=Plus(2),无可升
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(7);
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 2, 555L, 2)));
        SchoolStatusView.InviteBlock inv = svc().view(1L, IN_WINDOW).invite();
        assertThat(inv.count()).isEqualTo(7);
        SchoolStatusView.CardBlock c = inv.card();
        assertThat(c.tier()).isEqualTo(2);
        assertThat(c.tierName()).isEqualTo("Plus");
        assertThat(c.cardAmount()).isEqualTo(50);
        assertThat(c.claimableTier()).isEqualTo(2);  // = tier:无升档
        assertThat(c.resetsAvailable()).isEqualTo(3);
        assertThat(c.resetsUsed()).isEqualTo(2);
        assertThat(inv.kfcUnlocked()).isFalse();
    }

    @Test
    void upgradePendingWhenDeservedAboveOwned() {
        // 12 人还拿着 Go:应得 ProLite(3),卡块给出可升信息
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(12);
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 555L, 0)));
        SchoolStatusView.CardBlock c = svc().view(1L, IN_WINDOW).invite().card();
        assertThat(c.tier()).isEqualTo(1);
        assertThat(c.claimableTier()).isEqualTo(3);
        assertThat(c.claimableName()).isEqualTo("ProLite");
        assertThat(c.claimableCard()).isEqualTo(100);
        assertThat(c.resetsAvailable()).isEqualTo(9); // earned(12)=12-3=9,used=0
    }

    @Test
    void twentyInviteesUnlockKfcAndProAndCountUncapped() {
        when(read.firstCharge(anyLong())).thenReturn(Optional.empty());
        when(read.qualifiedInviteeCount(anyLong(), any(), any(), any())).thenReturn(23);
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 4, 555L, 0)));
        SchoolStatusView.InviteBlock inv = svc().view(1L, IN_WINDOW).invite();
        assertThat(inv.count()).isEqualTo(23);       // 计数不封顶
        assertThat(inv.kfcUnlocked()).isTrue();      // 20 人档
        assertThat(inv.card().tierName()).isEqualTo("Pro");
        assertThat(inv.card().resetsAvailable()).isEqualTo(19); // earned(23)=23-4=19
    }
}
