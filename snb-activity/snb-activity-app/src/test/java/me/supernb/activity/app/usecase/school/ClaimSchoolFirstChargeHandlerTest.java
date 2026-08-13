package me.supernb.activity.app.usecase.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolFirstChargeCommand;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.junit.jupiter.api.Test;

/// 首充礼领取:资格短路、幂等短路、占位→发卡→回写顺序、失败置 failed 且上抛、
/// failed 重试不重复占位、并发撞占位幂等回落。
class ClaimSchoolFirstChargeHandlerTest {

    private final SchoolStatusQueryService query = mock(SchoolStatusQueryService.class);
    private final SchoolClaimPort claims = mock(SchoolClaimPort.class);
    private final SubscriptionGrantPort grantPort = mock(SubscriptionGrantPort.class);

    private static SchoolSeasonProperties props() {
        return new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 3, "school-season");
    }

    private ClaimSchoolFirstChargeHandler handler(SubscriptionGrantPort port) {
        return new ClaimSchoolFirstChargeHandler(props(), query, claims, port);
    }

    /// 造一个 open 状态、首充块指定态的视图(邀请块空)。
    private static SchoolStatusView viewWithFirstCharge(int tierCard, String status) {
        return new SchoolStatusView(true, "9月1日 00:00",
                new SchoolStatusView.FirstChargeBlock(tierCard > 0, tierCard > 0, tierCard, "50.00", status),
                new SchoolStatusView.InviteBlock(0, List.of(), false));
    }

    private static SchoolClaimRecord pendingRec(long id, int tier, long groupId) {
        return new SchoolClaimRecord(id, 1L, SchoolClaimRecord.KIND_FIRST_CHARGE, tier, groupId,
                SchoolClaimRecord.STATUS_PENDING, 0, null);
    }

    @Test
    void closedWindowRejected() {
        when(query.view(1L)).thenReturn(SchoolStatusView.closed());
        assertThatThrownBy(() -> handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(grantPort);
    }

    @Test
    void ineligibleUserRejected() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(0, "none"));
        assertThatThrownBy(() -> handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(grantPort);
    }

    @Test
    void alreadyClaimedIsIdempotentNoRegrant() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(100, "claimed"));
        SchoolStatusView out = handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L));
        assertThat(out.firstCharge().status()).isEqualTo("claimed");
        verifyNoInteractions(grantPort);
        verify(claims, never()).insertPending(anyLong(), anyString(), anyInt(), anyLong());
    }

    @Test
    void happyPathInsertsPendingGrantsMarksSuccess() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(100, "claimable"));
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100)).thenReturn(Optional.empty());
        when(claims.insertPending(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100, 130L))
                .thenReturn(Optional.of(pendingRec(9L, 100, 130L)));
        when(grantPort.bulkGrant(List.of(1L), 130L, 3, "school-season"))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "created"), List.of()));

        handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L));

        verify(claims).insertPending(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100, 130L);
        verify(grantPort).bulkGrant(List.of(1L), 130L, 3, "school-season");
        verify(claims).markSuccess(9L);
    }

    @Test
    void reusedStatusAlsoCountsAsSuccess() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(50, "claimable"));
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50)).thenReturn(Optional.empty());
        when(claims.insertPending(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50, 129L))
                .thenReturn(Optional.of(pendingRec(9L, 50, 129L)));
        when(grantPort.bulkGrant(List.of(1L), 129L, 3, "school-season"))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "reused"), List.of()));

        handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L));
        verify(claims).markSuccess(9L);
    }

    @Test
    void grantFailureMarksFailedAndThrows() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(100, "claimable"));
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100)).thenReturn(Optional.empty());
        when(claims.insertPending(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100, 130L))
                .thenReturn(Optional.of(pendingRec(9L, 100, 130L)));
        when(grantPort.bulkGrant(List.of(1L), 130L, 3, "school-season"))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "failed"), List.of("boom")));

        assertThatThrownBy(() -> handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L)))
                .isInstanceOf(IllegalStateException.class);
        verify(claims).markFailed(anyLong(), anyString());
        verify(claims, never()).markSuccess(anyLong());
    }

    @Test
    void failedRowRetriesWithoutSecondInsert() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(100, "failed"));
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100)).thenReturn(Optional.of(
                new SchoolClaimRecord(9L, 1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100, 130L,
                        SchoolClaimRecord.STATUS_FAILED, 1, "boom")));
        when(grantPort.bulkGrant(List.of(1L), 130L, 3, "school-season"))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "created"), List.of()));

        handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L));

        verify(claims, never()).insertPending(anyLong(), anyString(), anyInt(), anyLong());
        verify(claims).markSuccess(9L);
    }

    @Test
    void concurrentDuplicateInsertFallsBackIdempotent() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(100, "claimable"));
        when(claims.find(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100)).thenReturn(Optional.empty());
        when(claims.insertPending(1L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100, 130L))
                .thenReturn(Optional.empty());   // 对手已占位

        handler(grantPort).handle(new ClaimSchoolFirstChargeCommand(1L));
        verifyNoInteractions(grantPort);
    }

    @Test
    void missingGrantPortThrows() {
        when(query.view(1L)).thenReturn(viewWithFirstCharge(100, "claimable"));
        assertThatThrownBy(() -> handler(null).handle(new ClaimSchoolFirstChargeCommand(1L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("未配置");
    }
}
