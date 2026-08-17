package me.supernb.activity.app.usecase.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolCardCommand;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.domain.model.school.SchoolCardRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolCardPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/// 开卡/升档编排:0 人拒领;开卡走 insert;升档 assign 新组+revoke 旧卡(revoke 失败容忍);
/// 应得 ≤ 已领幂等短路;跨档直升取应得最高档。
class ClaimSchoolCardHandlerTest {

    private final SchoolStatusQueryService query = mock(SchoolStatusQueryService.class);
    private final SchoolCardPort cards = mock(SchoolCardPort.class);
    private final SubscriptionGrantPort grantPort = mock(SubscriptionGrantPort.class);

    private static SchoolSeasonProperties props() {
        return new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 135L, "school-season");
    }

    private ClaimSchoolCardHandler handler() {
        return new ClaimSchoolCardHandler(props(), query, cards, grantPort);
    }

    /// 造一个 open 状态、指定合格数/卡态的视图。
    private static SchoolStatusView view(int count, int ownedTier) {
        String name = ownedTier > 0 ? SchoolSeasonProperties.CARD_NAMES[ownedTier - 1] : "";
        int amount = ownedTier > 0 ? SchoolSeasonProperties.CARD_AMOUNTS[ownedTier - 1] : 0;
        int deserved = SchoolSeasonProperties.deservedTier(count);
        return new SchoolStatusView(true, "9月1日 00:00",
                new SchoolStatusView.FirstChargeBlock(false, false, 0, "", "none"),
                new SchoolStatusView.InviteBlock(count,
                        new SchoolStatusView.CardBlock(ownedTier, name, amount,
                                deserved, "", 0, 0, 0, 0),
                        count >= 20));
    }

    @Test
    void zeroInviteesRejected() {
        when(query.view(1L)).thenReturn(view(0, 0));
        assertThatThrownBy(() -> handler().handle(new ClaimSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("先带 1 个");
        verify(grantPort, never()).assign(anyLong(), anyLong(), anyInt(), anyString());
    }

    @Test
    void closedRejected() {
        when(query.view(1L)).thenReturn(SchoolStatusView.closed());
        assertThatThrownBy(() -> handler().handle(new ClaimSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void firstClaimOpensGoCard() {
        when(query.view(1L)).thenReturn(view(1, 0));
        when(cards.find(1L)).thenReturn(Optional.empty());
        when(grantPort.assign(eq(1L), eq(132L), anyInt(), eq("school-season"))).thenReturn(777L);
        when(cards.insert(1L, 1, 777L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 777L, 0)));

        handler().handle(new ClaimSchoolCardCommand(1L));

        verify(cards).insert(1L, 1, 777L);
        verify(grantPort, never()).revoke(anyLong());
    }

    @Test
    void upgradeAssignsNewGroupThenRevokesOld() {
        // 5 人拿着 Go(sub 700)→ 升 Plus(组 133):先 assign 新卡再 revoke 旧订阅
        when(query.view(1L)).thenReturn(view(5, 1));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 2)));
        when(grantPort.assign(eq(1L), eq(133L), anyInt(), eq("school-season"))).thenReturn(701L);

        handler().handle(new ClaimSchoolCardCommand(1L));

        verify(cards).upgrade(9L, 2, 701L);
        verify(grantPort).revoke(700L);
    }

    @Test
    void skipTierJumpsToDeserved() {
        // 一口气进 11 人还没开过卡:直接开 ProLite(组 134),不发中间档
        when(query.view(1L)).thenReturn(view(11, 0));
        when(cards.find(1L)).thenReturn(Optional.empty());
        when(grantPort.assign(eq(1L), eq(134L), anyInt(), anyString())).thenReturn(702L);
        when(cards.insert(1L, 3, 702L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 3, 702L, 0)));

        handler().handle(new ClaimSchoolCardCommand(1L));

        verify(cards).insert(1L, 3, 702L);
        verify(grantPort, never()).assign(anyLong(), eq(132L), anyInt(), anyString());
    }

    @Test
    void deservedEqualsOwnedIsIdempotentNoop() {
        when(query.view(1L)).thenReturn(view(7, 2));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 2, 700L, 0)));

        handler().handle(new ClaimSchoolCardCommand(1L));

        verify(grantPort, never()).assign(anyLong(), anyLong(), anyInt(), anyString());
        verify(cards, never()).upgrade(anyLong(), anyInt(), anyLong());
    }

    @Test
    void revokeFailureIsTolerated() {
        when(query.view(1L)).thenReturn(view(5, 1));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 0)));
        when(grantPort.assign(eq(1L), eq(133L), anyInt(), anyString())).thenReturn(701L);
        Mockito.doThrow(new RuntimeException("boom")).when(grantPort).revoke(700L);

        // revoke 失败不影响升档结果(旧卡多活几天无害)
        handler().handle(new ClaimSchoolCardCommand(1L));
        verify(cards).upgrade(9L, 2, 701L);
    }

    @Test
    void assignFailureSurfacesAndLeavesCardUntouched() {
        when(query.view(1L)).thenReturn(view(1, 0));
        when(cards.find(1L)).thenReturn(Optional.empty());
        when(grantPort.assign(anyLong(), anyLong(), anyInt(), anyString()))
                .thenThrow(new RuntimeException("sub2api down"));

        assertThatThrownBy(() -> handler().handle(new ClaimSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("稍后重试");
        verify(cards, never()).insert(anyLong(), anyInt(), anyLong());
    }

    @Test
    void nullGrantPortFailsLoud() {
        when(query.view(1L)).thenReturn(view(1, 0));
        when(cards.find(1L)).thenReturn(Optional.empty());
        ClaimSchoolCardHandler h = new ClaimSchoolCardHandler(props(), query, cards, (SubscriptionGrantPort) null);
        assertThatThrownBy(() -> h.handle(new ClaimSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("发卡通道未配置");
    }
}
