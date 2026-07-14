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
/// (靠 `@ConditionalOnProperty` 装配期条件 fail-closed:`sub2api.admin-key` 必须在
/// Environment 中真正缺席——`application.yml` 绝不能给它 yml 默认值,否则该注解无
/// `havingValue` 时空串也判匹配,两个 Bean 会恒装配,2026-07-14 曾踩坑并修复)。
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
