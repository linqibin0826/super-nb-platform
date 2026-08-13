package me.supernb.activity.app.usecase.school;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.supernb.activity.app.usecase.school.command.ClaimSchoolMilestoneCommand;
import me.supernb.activity.app.usecase.school.config.SchoolSeasonProperties;
import me.supernb.activity.app.usecase.school.query.SchoolStatusQueryService;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.activity.domain.port.school.SchoolClaimPort;
import org.junit.jupiter.api.Test;

/// 里程碑领取:tier 白名单(KFC 档 10 无 claim 路)、未解锁拒领、各档独立可领、组映射正确。
class ClaimSchoolMilestoneHandlerTest {

    private final SchoolStatusQueryService query = mock(SchoolStatusQueryService.class);
    private final SchoolClaimPort claims = mock(SchoolClaimPort.class);
    private final SubscriptionGrantPort grantPort = mock(SubscriptionGrantPort.class);

    private static SchoolSeasonProperties props() {
        return new SchoolSeasonProperties(
                "2026-08-13T04:00:00Z", "2026-08-31T16:00:00Z", "",
                129L, 130L, 131L, 132L, 133L, 134L, 3, "school-season");
    }

    private ClaimSchoolMilestoneHandler handler() {
        return new ClaimSchoolMilestoneHandler(props(), query, claims, grantPort);
    }

    /// 造一个 open 视图:count 个合格被邀,里程碑解锁态按 count 推,tier1 领取态可指定。
    private static SchoolStatusView viewWithInvites(int count, String tier1Status) {
        List<SchoolStatusView.Milestone> ms = List.of(
                new SchoolStatusView.Milestone(1, 50, count >= 1, count >= 1 ? tier1Status : "none"),
                new SchoolStatusView.Milestone(3, 100, count >= 3, count >= 3 ? "claimable" : "none"),
                new SchoolStatusView.Milestone(6, 200, count >= 6, count >= 6 ? "claimable" : "none"));
        return new SchoolStatusView(true, "9月1日 00:00",
                new SchoolStatusView.FirstChargeBlock(false, false, 0, "", "none"),
                new SchoolStatusView.InviteBlock(count, ms, count >= 10));
    }

    @Test
    void kfcTierHasNoClaimEndpoint() {
        assertThatThrownBy(() -> handler().handle(new ClaimSchoolMilestoneCommand(1L, 10)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(grantPort);
    }

    @Test
    void unknownTierRejected() {
        assertThatThrownBy(() -> handler().handle(new ClaimSchoolMilestoneCommand(1L, 2)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(grantPort);
    }

    @Test
    void lockedTierRejected() {
        when(query.view(1L)).thenReturn(viewWithInvites(2, "claimable"));
        assertThatThrownBy(() -> handler().handle(new ClaimSchoolMilestoneCommand(1L, 3)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(grantPort);
    }

    @Test
    void tier3GrantsGroup133() {
        when(query.view(1L)).thenReturn(viewWithInvites(4, "claimed"));
        when(claims.find(1L, SchoolClaimRecord.KIND_MILESTONE, 3)).thenReturn(Optional.empty());
        when(claims.insertPending(1L, SchoolClaimRecord.KIND_MILESTONE, 3, 133L))
                .thenReturn(Optional.of(new SchoolClaimRecord(9L, 1L, SchoolClaimRecord.KIND_MILESTONE,
                        3, 133L, SchoolClaimRecord.STATUS_PENDING, 0, null)));
        when(grantPort.bulkGrant(List.of(1L), 133L, 3, "school-season"))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "created"), List.of()));

        handler().handle(new ClaimSchoolMilestoneCommand(1L, 3));

        verify(grantPort).bulkGrant(List.of(1L), 133L, 3, "school-season");
        verify(claims).markSuccess(9L);
    }

    @Test
    void eachTierIndependentlyClaimable() {
        // tier1 已领(claimed),tier3 解锁未领 → tier3 照常发,互不影响
        when(query.view(1L)).thenReturn(viewWithInvites(3, "claimed"));
        when(claims.find(1L, SchoolClaimRecord.KIND_MILESTONE, 3)).thenReturn(Optional.empty());
        when(claims.insertPending(1L, SchoolClaimRecord.KIND_MILESTONE, 3, 133L))
                .thenReturn(Optional.of(new SchoolClaimRecord(9L, 1L, SchoolClaimRecord.KIND_MILESTONE,
                        3, 133L, SchoolClaimRecord.STATUS_PENDING, 0, null)));
        when(grantPort.bulkGrant(List.of(1L), 133L, 3, "school-season"))
                .thenReturn(new SubscriptionGrantOutcome(Map.of(1L, "created"), List.of()));

        handler().handle(new ClaimSchoolMilestoneCommand(1L, 3));
        verify(claims).markSuccess(9L);
    }
}
