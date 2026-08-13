package me.supernb.activity.app.usecase.school;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import me.supernb.activity.app.usecase.school.command.ResetSchoolCardCommand;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.domain.model.school.SchoolCardRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolCardPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/// 重置银行编排:无卡拒;次数不足拒(earned 推导);先原子扣再调 reset-quota;
/// 下游失败回补次数并抛(次数不丢)。
class ResetSchoolCardHandlerTest {

    private final SchoolStatusQueryService query = mock(SchoolStatusQueryService.class);
    private final SchoolCardPort cards = mock(SchoolCardPort.class);
    private final SubscriptionGrantPort grantPort = mock(SubscriptionGrantPort.class);

    private ResetSchoolCardHandler handler() {
        return new ResetSchoolCardHandler(query, cards, grantPort);
    }

    /// open 视图,合格数=count(重置获得侧由 handler 自己推导)。
    private static SchoolStatusView view(int count) {
        return new SchoolStatusView(true, "9月1日 00:00",
                new SchoolStatusView.FirstChargeBlock(false, false, 0, "", "none"),
                new SchoolStatusView.InviteBlock(count, SchoolStatusView.CardBlock.empty(), false));
    }

    @Test
    void noCardRejected() {
        when(query.view(1L)).thenReturn(view(3));
        when(cards.find(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler().handle(new ResetSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("还没开卡");
    }

    @Test
    void exhaustedBankRejectedBeforeTouchingAnything() {
        // 3 人:earned=2,已用 2 → 拒,不碰扣减也不打下游
        when(query.view(1L)).thenReturn(view(3));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 2)));
        assertThatThrownBy(() -> handler().handle(new ResetSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重置次数不够");
        verify(cards, never()).consumeReset(anyLong(), anyInt());
        verify(grantPort, never()).resetQuota(anyLong());
    }

    @Test
    void consumeThenResetQuota() {
        // 4 人:earned=3,已用 1 → 扣到后打 reset-quota
        when(query.view(1L)).thenReturn(view(4));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 1)));
        when(cards.consumeReset(9L, 3)).thenReturn(true);

        handler().handle(new ResetSchoolCardCommand(1L));

        verify(cards).consumeReset(9L, 3);
        verify(grantPort).resetQuota(700L);
        verify(cards, never()).refundReset(anyLong());
    }

    @Test
    void concurrentLoserGetsRejectedByAtomicConsume() {
        when(query.view(1L)).thenReturn(view(4));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 1)));
        when(cards.consumeReset(9L, 3)).thenReturn(false);   // 并发对手先扣走了最后一枚
        assertThatThrownBy(() -> handler().handle(new ResetSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重置次数不够");
        verify(grantPort, never()).resetQuota(anyLong());
    }

    @Test
    void downstreamFailureRefundsTheCredit() {
        when(query.view(1L)).thenReturn(view(4));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 1)));
        when(cards.consumeReset(9L, 3)).thenReturn(true);
        Mockito.doThrow(new RuntimeException("sub2api down")).when(grantPort).resetQuota(700L);

        assertThatThrownBy(() -> handler().handle(new ResetSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("次数已退回");
        verify(cards).refundReset(9L);
    }

    @Test
    void nullGrantPortFailsLoudWithoutConsuming() {
        when(query.view(1L)).thenReturn(view(4));
        when(cards.find(1L)).thenReturn(Optional.of(new SchoolCardRecord(9L, 1L, 1, 700L, 1)));
        ResetSchoolCardHandler h = new ResetSchoolCardHandler(query, cards, (SubscriptionGrantPort) null);
        assertThatThrownBy(() -> h.handle(new ResetSchoolCardCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("重置通道未配置");
        verify(cards, never()).consumeReset(anyLong(), anyInt());
    }
}
