package me.supernb.activity.infra.adapter.subscription;

import java.util.List;
import me.supernb.activity.domain.model.checkin.SubscriptionGrantOutcome;
import me.supernb.activity.domain.port.checkin.SubscriptionGrantPort;
import me.supernb.sub2api.admin.Sub2apiAdminSubscriptionClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/// SubscriptionGrantPort 实现:零逻辑薄委托 Sub2apiAdminSubscriptionClient。条件与
/// `Sub2apiAdminAutoConfiguration` 的 `sub2api.admin-key` 保持一致——不配 admin-key 时
/// 客户端 Bean 本就不存在,本适配器同步不装配,避免整个 Spring 上下文因缺 Bean 装配失败
/// (照 content.admin-token 的 fail-closed 惯例:能力缺省关闭,不拖垮全局启动)。
@Component
@ConditionalOnProperty(prefix = "sub2api", name = "admin-key")
public class SubscriptionGrantAdapter implements SubscriptionGrantPort {

    private final Sub2apiAdminSubscriptionClient client;

    /// 构造:注入 starter 装配的 admin 订阅客户端(仅在配置 sub2api.admin-key 时存在)。
    public SubscriptionGrantAdapter(Sub2apiAdminSubscriptionClient client) {
        this.client = client;
    }

    @Override
    public SubscriptionGrantOutcome bulkGrant(List<Long> userIds, long groupId, int validityDays, String notes) {
        var result = client.bulkAssign(userIds, groupId, validityDays, notes);
        return new SubscriptionGrantOutcome(result.statuses(), result.errors());
    }
}
