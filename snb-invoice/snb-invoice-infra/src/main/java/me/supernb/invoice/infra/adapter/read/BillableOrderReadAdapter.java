package me.supernb.invoice.infra.adapter.read;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.sub2api.invoice.InvoiceOrderReadModel;
import org.springframework.stereotype.Component;

/// BillableOrderReadPort 实现:薄委托防腐层 InvoiceOrderReadModel,上游 DTO → 域内 OrderLine。
@Component
public class BillableOrderReadAdapter implements BillableOrderReadPort {

    private final InvoiceOrderReadModel readModel;

    /// 构造:注入防腐层读模型(read-datasource 配置激活)。
    public BillableOrderReadAdapter(InvoiceOrderReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public List<OrderLine> completedOrders(long userId) {
        return readModel.completedBalanceOrders(userId).stream()
                .map(r -> new OrderLine(r.id(), r.orderNo(), r.amount(), r.completedAt()))
                .toList();
    }

    @Override
    public BigDecimal balanceOf(long userId) {
        return readModel.balanceOf(userId);
    }

    @Override
    public Map<Long, String> emailsByIds(Collection<Long> userIds) {
        return readModel.emailsByIds(userIds);
    }
}
