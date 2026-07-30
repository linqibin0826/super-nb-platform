package me.supernb.activity.app.usecase.thursday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import me.supernb.activity.app.usecase.thursday.command.ClaimThursdayBucketCommand;
import me.supernb.activity.app.usecase.thursday.config.ThursdayProperties;
import me.supernb.activity.app.usecase.thursday.query.ThursdayBucketQueryService;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import org.junit.jupiter.api.Test;

/// 领取编排。**每条断言都在守一次白发或漏发**:不达标不发、已领不重发、非场次不发、
/// 发卡通道缺席/发卡未成功一律炸出来(绝不静默成"看起来领到了")。
class ClaimThursdayBucketHandlerTest {

    private final ThursdayBucketQueryService query = mock(ThursdayBucketQueryService.class);
    private final SubscriptionGrantPort grantPort = mock(SubscriptionGrantPort.class);

    private ClaimThursdayBucketHandler handler(SubscriptionGrantPort port) {
        return new ClaimThursdayBucketHandler(
                new ThursdayProperties("", new BigDecimal("50"), 50, 1, "opening-fk", 3, "salt", "22:00", "20:00", new BigDecimal("30")), query, port);
    }

    private void viewIs(ThursdayBucketView v) {
        when(query.view(7L)).thenReturn(v);
        when(query.groupIdToday()).thenReturn(123L);
    }

    @Test
    void ineligibleUserGetsNoCard() {
        viewIs(new ThursdayBucketView(true, false, false, null, 3, 50, null, false, "22:00"));
        ThursdayBucketView r = handler(grantPort).handle(new ClaimThursdayBucketCommand(7));
        assertThat(r.claimed()).isFalse();
        verifyNoInteractions(grantPort);
    }

    @Test
    void nonSessionDayGetsNoCard() {
        viewIs(ThursdayBucketView.closed(50));
        handler(grantPort).handle(new ClaimThursdayBucketCommand(7));
        verifyNoInteractions(grantPort);
    }

    /// 已领过的人再点,不再打一发 admin API(bulk-assign 虽幂等,但白白的抖动面)。
    @Test
    void alreadyClaimedDoesNotCallAdminApiAgain() {
        viewIs(new ThursdayBucketView(true, true, true, 2, 3, 50, null, false, "22:00"));
        ThursdayBucketView r = handler(grantPort).handle(new ClaimThursdayBucketCommand(7));
        assertThat(r.claimed()).isTrue();
        assertThat(r.bucketNo()).isEqualTo(2);
        verifyNoInteractions(grantPort);
    }

    @Test
    void eligibleUserGetsCardWithFixedNotesAndSessionGroup() {
        viewIs(new ThursdayBucketView(true, true, false, 2, 3, 50, null, false, "22:00"));
        when(grantPort.bulkGrant(any(), anyLong(), anyInt(), any()))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(7L, "created"), List.of()));

        ThursdayBucketView r = handler(grantPort).handle(new ClaimThursdayBucketCommand(7));

        assertThat(r.claimed()).isTrue();
        assertThat(r.bucketNo()).isEqualTo(2);
        verify(grantPort).bulkGrant(List.of(7L), 123L, 1, "opening-fk");
    }

    /// 并发双击的另一半:sub2api 回 reused 也算领到了,不能当失败炸给用户。
    @Test
    void reusedCountsAsClaimed() {
        viewIs(new ThursdayBucketView(true, true, false, 1, 3, 50, null, false, "22:00"));
        when(grantPort.bulkGrant(any(), anyLong(), anyInt(), any()))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(7L, "reused"), List.of()));
        assertThat(handler(grantPort).handle(new ClaimThursdayBucketCommand(7)).claimed()).isTrue();
    }

    /// 🚨 admin-key 没配 → 必须炸。反面教材:签到发放曾因此静默断(runbook 33)。
    @Test
    void missingGrantPortThrowsInsteadOfSilentlySucceeding() {
        viewIs(new ThursdayBucketView(true, true, false, 1, 3, 50, null, false, "22:00"));
        assertThatThrownBy(() -> handler(null).handle(new ClaimThursdayBucketCommand(7)))
                .isInstanceOf(IllegalStateException.class);
    }

    /// 🚨 发卡回 failed（或压根没回这个 uid）→ 必须炸,绝不能回一个 claimed=true 的假成功。
    @Test
    void failedGrantThrows() {
        viewIs(new ThursdayBucketView(true, true, false, 1, 3, 50, null, false, "22:00"));
        when(grantPort.bulkGrant(any(), anyLong(), anyInt(), any()))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(7L, "failed"), List.of("409 conflict")));
        assertThatThrownBy(() -> handler(grantPort).handle(new ClaimThursdayBucketCommand(7)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingStatusForUserThrows() {
        viewIs(new ThursdayBucketView(true, true, false, 1, 3, 50, null, false, "22:00"));
        when(grantPort.bulkGrant(any(), anyLong(), anyInt(), any()))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(), List.of()));
        assertThatThrownBy(() -> handler(grantPort).handle(new ClaimThursdayBucketCommand(7)))
                .isInstanceOf(IllegalStateException.class);
    }

    /// 判定与发卡之间跨了零点(场次翻篇):宁可这次领取落空重来,也不拿 null 分组去发卡。
    @Test
    void sessionRollingOverBetweenCheckAndGrantAborts() {
        when(query.view(7L)).thenReturn(new ThursdayBucketView(true, true, false, 1, 3, 50, null, false, "22:00"));
        when(query.groupIdToday()).thenReturn(null);
        ThursdayBucketView r = handler(grantPort).handle(new ClaimThursdayBucketCommand(7));
        assertThat(r.open()).isFalse();
        verify(grantPort, never()).bulkGrant(any(), anyLong(), anyInt(), any());
    }
}
