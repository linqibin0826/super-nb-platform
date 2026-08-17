package me.supernb.ops.app.usecase.query;

import java.util.List;
import me.supernb.ops.app.usecase.query.view.AccountView;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import org.springframework.stereotype.Service;

/// 账号列表/单查(视图无密文)。
@Service
public class OpsAccountQueryService {

    private final OpsAccountRepository accounts;

    /// 构造:注入账号仓储端口。
    public OpsAccountQueryService(OpsAccountRepository accounts) {
        this.accounts = accounts;
    }

    public List<AccountView> listAll() {
        return accounts.listAll().stream().map(AccountView::of).toList();
    }

    public AccountView get(long id) {
        return accounts.find(id).map(AccountView::of).orElseThrow(() -> OpsException.accountNotFound(id));
    }
}
