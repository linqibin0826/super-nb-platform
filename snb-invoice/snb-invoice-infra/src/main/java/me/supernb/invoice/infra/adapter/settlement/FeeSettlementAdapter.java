package me.supernb.invoice.infra.adapter.settlement;

import java.math.BigDecimal;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.settlement.FeeSettlementPort;
import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/// FeeSettlementPort 实现:经 sub2api admin API 扣/退余额。客户端用 ObjectProvider 延迟解析——
/// admin-key 未配时 Bean 缺席,调用时 fail-closed(上下文照常起,WiringTest/本地无 key 可跑读链路);
/// 上游明确拒绝翻译成 settlementFailed(409,报文透给管理员);连接类故障原样上抛(500,可重试)。
@Component
public class FeeSettlementAdapter implements FeeSettlementPort {

    private final ObjectProvider<Sub2apiAdminBalanceClient> client;

    /// 构造:注入余额客户端的 ObjectProvider。
    public FeeSettlementAdapter(ObjectProvider<Sub2apiAdminBalanceClient> client) {
        this.client = client;
    }

    @Override
    public void charge(long userId, BigDecimal amount, String note) {
        resolve().ifPresentOrElse(
                c -> translate(() -> c.subtract(userId, amount, note)),
                () -> {
                    throw InvoiceException.settlementFailed("sub2api admin 通道未配置(缺 SUB2API_ADMIN_KEY)");
                });
    }

    @Override
    public void refund(long userId, BigDecimal amount, String note) {
        resolve().ifPresentOrElse(
                c -> translate(() -> c.add(userId, amount, note)),
                () -> {
                    throw InvoiceException.settlementFailed("sub2api admin 通道未配置(缺 SUB2API_ADMIN_KEY)");
                });
    }

    private java.util.Optional<Sub2apiAdminBalanceClient> resolve() {
        return java.util.Optional.ofNullable(client.getIfAvailable());
    }

    private static void translate(Runnable call) {
        try {
            call.run();
        } catch (Sub2apiAdminBalanceClient.BalanceOperationException e) {
            throw InvoiceException.settlementFailed(e.getMessage());
        }
    }
}
