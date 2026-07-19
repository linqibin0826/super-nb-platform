package me.supernb.activity.infra.adapter.external;

import java.util.List;
import me.supernb.activity.domain.port.raffle.RaffleRedeemCodeIssuerPort;
import me.supernb.sub2api.admin.Sub2apiAdminRedeemCodeClient;
import org.springframework.stereotype.Component;

/// RaffleRedeemCodeIssuerPort 实现:零逻辑薄委托 snb-sub2api 的 admin 兑换码客户端。
@Component
public class Sub2apiRaffleRedeemCodeIssuerAdapter implements RaffleRedeemCodeIssuerPort {

    private final Sub2apiAdminRedeemCodeClient client;

    public Sub2apiRaffleRedeemCodeIssuerAdapter(Sub2apiAdminRedeemCodeClient client) {
        this.client = client;
    }

    @Override
    public List<String> issue(long groupId, int validityDays, int count) {
        return client.generateSubscriptionCodes(groupId, validityDays, count);
    }
}
