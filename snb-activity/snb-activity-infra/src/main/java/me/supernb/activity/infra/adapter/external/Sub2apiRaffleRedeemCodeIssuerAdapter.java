package me.supernb.activity.infra.adapter.external;

import java.util.List;
import me.supernb.activity.domain.exception.RaffleRedeemCodeChannelUnavailableException;
import me.supernb.activity.domain.port.raffle.RaffleRedeemCodeIssuerPort;
import me.supernb.sub2api.admin.Sub2apiAdminRedeemCodeClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/// RaffleRedeemCodeIssuerPort 实现:经 sub2api admin API 签发兑换码。客户端用 ObjectProvider
/// 延迟解析——admin-key 未配时 Bean 缺席,调用时才 fail-closed(照 FeeSettlementAdapter 的既有
/// 模式:上下文照常起,WiringTest/本地无 key 可跑读链路,只有真正调用生成兑换码才会报错)。
@Component
public class Sub2apiRaffleRedeemCodeIssuerAdapter implements RaffleRedeemCodeIssuerPort {

    private final ObjectProvider<Sub2apiAdminRedeemCodeClient> client;

    public Sub2apiRaffleRedeemCodeIssuerAdapter(ObjectProvider<Sub2apiAdminRedeemCodeClient> client) {
        this.client = client;
    }

    @Override
    public List<String> issue(long groupId, int validityDays, int count) {
        Sub2apiAdminRedeemCodeClient c = client.getIfAvailable();
        if (c == null) {
            throw new RaffleRedeemCodeChannelUnavailableException();
        }
        return c.generateSubscriptionCodes(groupId, validityDays, count);
    }
}
