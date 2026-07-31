package me.supernb.activity.infra.adapter.subscription;

import java.math.BigDecimal;
import me.supernb.activity.domain.port.checkin.BalanceGrantPort;
import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/// BalanceGrantPort 实现:零逻辑薄委托 Sub2apiAdminBalanceClient。条件与
/// `Sub2apiAdminAutoConfiguration` 及 SubscriptionGrantAdapter 保持一致——不配 admin-key 时
/// 客户端 Bean 本就不存在,本适配器同步不装配,避免整个 Spring 上下文因缺 Bean 装配失败。
/// ⚠️ `application.yml` 绝不能给 `sub2api.admin-key` 默认值,否则该注解无 havingValue 时
/// 空串也判匹配、条件恒成立(2026-07-14 曾踩坑并修复)。
@Component
@ConditionalOnProperty(prefix = "sub2api", name = "admin-key")
public class BalanceGrantAdapter implements BalanceGrantPort {

    private final Sub2apiAdminBalanceClient client;

    /// 构造:注入 starter 装配的 admin 余额客户端(仅在配置 sub2api.admin-key 时存在)。
    public BalanceGrantAdapter(Sub2apiAdminBalanceClient client) {
        this.client = client;
    }

    @Override
    public void grant(long userId, BigDecimal amountCny, String notes) {
        client.add(userId, amountCny, notes);
    }
}
