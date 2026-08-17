package me.supernb.ops.app.usecase.query;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import me.supernb.ops.app.usecase.query.view.SubscriptionView;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountRow;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionRow;
import org.springframework.stereotype.Service;

/// 订阅列表(全量/按账号),email 冗余列在内存里 join(库量小)。
@Service
public class OpsSubscriptionQueryService {

    private final OpsAccountRepository accounts;
    private final OpsSubscriptionRepository subscriptions;

    /// 构造:注入两仓储端口。
    public OpsSubscriptionQueryService(OpsAccountRepository accounts, OpsSubscriptionRepository subscriptions) {
        this.accounts = accounts;
        this.subscriptions = subscriptions;
    }

    public List<SubscriptionView> listAll() {
        return join(subscriptions.listAll());
    }

    public List<SubscriptionView> listByAccount(long accountId) {
        return join(subscriptions.listByAccount(accountId));
    }

    private List<SubscriptionView> join(List<SubscriptionRow> rows) {
        Map<Long, String> emails = accounts.listAll().stream()
                .collect(Collectors.toMap(AccountRow::id, r -> r.data().email()));
        return rows.stream().map(r -> SubscriptionView.of(r, emails.getOrDefault(r.data().accountId(), "?")))
                .toList();
    }
}
