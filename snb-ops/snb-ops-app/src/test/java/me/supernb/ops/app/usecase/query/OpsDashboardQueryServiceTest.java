package me.supernb.ops.app.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.query.view.DashboardView;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 看板筛选:扣款窗口 [today, today+30] 闭区间且仅 ACTIVE、退款跟进日 ≤today 且五态未结案、
/// 封号未结案计数;扣款列表按日升序。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class OpsDashboardQueryServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 8, 17);
    static final Instant NOW = Instant.parse("2026-08-17T00:00:00Z");

    final OpsAccountRepository accounts = mock(OpsAccountRepository.class);
    final OpsSubscriptionRepository subscriptions = mock(OpsSubscriptionRepository.class);
    final OpsDashboardQueryService service = new OpsDashboardQueryService(accounts, subscriptions);

    static AccountRow account(long id, String email) {
        return new AccountRow(id, new AccountData(email, "gmail", null, null, null, null, null, null,
                AccountStatus.ACTIVE, null, null), NOW, NOW);
    }

    static SubscriptionRow sub(long id, SubStatus status, LocalDate nextBillingAt,
            RefundStatus refund, LocalDate followUpAt) {
        return new SubscriptionRow(id, new SubscriptionData(1L, SubService.CHATGPT, null, null, null, null,
                null, null, null, null, nextBillingAt, null, status, null, null,
                status == SubStatus.BANNED ? NOW : null, null,
                refund, null, null, null, followUpAt, null, null), NOW, NOW);
    }

    void stubAccounts() {
        when(accounts.listAll()).thenReturn(List.of(account(1L, "a@gmail.com")));
    }

    @Test
    void billingWindowIsInclusiveThirtyDaysForActiveOnly() {
        stubAccounts();
        when(subscriptions.listAll()).thenReturn(List.of(
                sub(1, SubStatus.ACTIVE, TODAY, RefundStatus.NONE, null),                // 在列(下界)
                sub(2, SubStatus.ACTIVE, TODAY.plusDays(30), RefundStatus.NONE, null),   // 在列(上界)
                sub(3, SubStatus.ACTIVE, TODAY.plusDays(31), RefundStatus.NONE, null),   // 出界
                sub(4, SubStatus.ACTIVE, TODAY.minusDays(1), RefundStatus.NONE, null),   // 已过不算临近
                sub(5, SubStatus.EXPIRED, TODAY.plusDays(5), RefundStatus.NONE, null),   // 非 ACTIVE
                sub(6, SubStatus.ACTIVE, null, RefundStatus.NONE, null)));               // 无扣款日
        DashboardView view = service.overview(TODAY);
        assertThat(view.upcomingBilling()).extracting(v -> v.id()).containsExactly(1L, 2L);
    }

    @Test
    void refundFollowUpsDueTodayOrEarlierAndOpenStatusOnly() {
        stubAccounts();
        when(subscriptions.listAll()).thenReturn(List.of(
                sub(1, SubStatus.BANNED, null, RefundStatus.PENDING, TODAY),             // 在列
                sub(2, SubStatus.BANNED, null, RefundStatus.APPEALING, TODAY.minusDays(3)), // 在列
                sub(3, SubStatus.BANNED, null, RefundStatus.PENDING, TODAY.plusDays(1)), // 未到期
                sub(4, SubStatus.BANNED, null, RefundStatus.REFUNDED, TODAY.minusDays(1)), // 已结案
                sub(5, SubStatus.BANNED, null, RefundStatus.PENDING, null)));            // 无跟进日
        DashboardView view = service.overview(TODAY);
        assertThat(view.refundFollowUps()).extracting(v -> v.id()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void bannedOpenCountsBannedWithOpenRefund() {
        stubAccounts();
        when(subscriptions.listAll()).thenReturn(List.of(
                sub(1, SubStatus.BANNED, null, RefundStatus.PENDING, null),
                sub(2, SubStatus.BANNED, null, RefundStatus.APPEALING, null),
                sub(3, SubStatus.BANNED, null, RefundStatus.REFUNDED, null),
                sub(4, SubStatus.ACTIVE, null, RefundStatus.PENDING, null)));
        assertThat(service.overview(TODAY).bannedOpenCount()).isEqualTo(2);
    }

    @Test
    void upcomingSortedByBillingDateAscendingWithEmailJoined() {
        stubAccounts();
        when(subscriptions.listAll()).thenReturn(List.of(
                sub(1, SubStatus.ACTIVE, TODAY.plusDays(9), RefundStatus.NONE, null),
                sub(2, SubStatus.ACTIVE, TODAY.plusDays(2), RefundStatus.NONE, null)));
        DashboardView view = service.overview(TODAY);
        assertThat(view.upcomingBilling()).extracting(v -> v.id()).containsExactly(2L, 1L);
        assertThat(view.upcomingBilling().getFirst().email()).isEqualTo("a@gmail.com");
    }
}
