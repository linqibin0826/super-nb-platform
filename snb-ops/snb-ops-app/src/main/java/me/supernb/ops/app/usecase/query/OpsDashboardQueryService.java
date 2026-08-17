package me.supernb.ops.app.usecase.query;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.supernb.ops.app.usecase.query.view.DashboardView;
import me.supernb.ops.app.usecase.query.view.SubscriptionView;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionRow;
import org.springframework.stereotype.Service;

/// 看板待办筛选(纯内存):扣款窗口 [today, today+30] 闭区间且仅 ACTIVE;
/// 退款跟进日 ≤today 且五态 ∈ {PENDING, APPEALING};封号未结案 = BANNED ∧ 退款未结案。
@Service
public class OpsDashboardQueryService {

    private final OpsAccountRepository accounts;
    private final OpsSubscriptionRepository subscriptions;

    /// 构造:注入两仓储端口。
    public OpsDashboardQueryService(OpsAccountRepository accounts, OpsSubscriptionRepository subscriptions) {
        this.accounts = accounts;
        this.subscriptions = subscriptions;
    }

    public DashboardView overview(LocalDate today) {
        Map<Long, String> emails = accounts.listAll().stream()
                .collect(Collectors.toMap(AccountRow::id, r -> r.data().email()));
        List<SubscriptionRow> all = subscriptions.listAll();
        LocalDate horizon = today.plusDays(30);
        List<SubscriptionView> upcoming = all.stream()
                .filter(r -> r.data().status() == SubStatus.ACTIVE)
                .filter(r -> r.data().nextBillingAt() != null
                        && !r.data().nextBillingAt().isBefore(today)
                        && !r.data().nextBillingAt().isAfter(horizon))
                .sorted(Comparator.comparing(r -> r.data().nextBillingAt()))
                .map(r -> toView(r, emails)).toList();
        List<SubscriptionView> followUps = all.stream()
                .filter(r -> isOpenRefund(r.data().refundStatus()))
                .filter(r -> r.data().refundFollowUpAt() != null && !r.data().refundFollowUpAt().isAfter(today))
                .map(r -> toView(r, emails)).toList();
        long bannedOpen = all.stream()
                .filter(r -> r.data().status() == SubStatus.BANNED)
                .filter(r -> isOpenRefund(r.data().refundStatus()))
                .count();
        return new DashboardView(upcoming, followUps, bannedOpen);
    }

    private static boolean isOpenRefund(RefundStatus s) {
        return s == RefundStatus.PENDING || s == RefundStatus.APPEALING;
    }

    private static SubscriptionView toView(SubscriptionRow row, Map<Long, String> emails) {
        return SubscriptionView.of(row, emails.getOrDefault(row.data().accountId(), "?"));
    }
}
