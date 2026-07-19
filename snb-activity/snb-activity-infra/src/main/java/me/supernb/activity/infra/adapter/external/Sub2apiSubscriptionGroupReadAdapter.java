package me.supernb.activity.infra.adapter.external;

import java.util.List;
import me.supernb.activity.domain.exception.RaffleRedeemCodeChannelUnavailableException;
import me.supernb.activity.domain.model.read.raffle.SubscriptionGroupView;
import me.supernb.activity.domain.port.read.SubscriptionGroupReadPort;
import me.supernb.sub2api.admin.Sub2apiAdminGroupClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/// SubscriptionGroupReadPort 实现:经 sub2api admin API 列分组。客户端 ObjectProvider
/// 延迟解析(照 Sub2apiRaffleRedeemCodeIssuerAdapter 的既有模式:admin-key 未配时 Bean
/// 缺席,上下文照常起,调用时才 fail-closed)——分组下拉与发码同属一条 admin 通道,
/// 通道缺席复用同一个 DEP_UNAVAILABLE 异常。
@Component
public class Sub2apiSubscriptionGroupReadAdapter implements SubscriptionGroupReadPort {

    private final ObjectProvider<Sub2apiAdminGroupClient> client;

    public Sub2apiSubscriptionGroupReadAdapter(ObjectProvider<Sub2apiAdminGroupClient> client) {
        this.client = client;
    }

    @Override
    public List<SubscriptionGroupView> listForRedeemCode() {
        Sub2apiAdminGroupClient c = client.getIfAvailable();
        if (c == null) {
            throw new RaffleRedeemCodeChannelUnavailableException();
        }
        return c.listActiveSubscriptionGroups().stream()
                .map(g -> new SubscriptionGroupView(g.id(), g.name()))
                .toList();
    }
}
