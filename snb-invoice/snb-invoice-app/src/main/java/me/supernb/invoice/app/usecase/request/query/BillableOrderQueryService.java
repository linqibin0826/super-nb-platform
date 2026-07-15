package me.supernb.invoice.app.usecase.request.query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import me.supernb.invoice.app.usecase.request.dto.BillableOverview;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import org.springframework.stereotype.Service;

/// 可开票总览查询:上游全部已完成余额单 - 本库有效占用 = 可勾选;附全选合计与当前余额。
@Service
public class BillableOrderQueryService {

    private final BillableOrderReadPort billableOrders;
    private final InvoiceRequestReadPort requestRead;

    /// 构造:注入上游订单读端口与本库申请读端口。
    public BillableOrderQueryService(BillableOrderReadPort billableOrders, InvoiceRequestReadPort requestRead) {
        this.billableOrders = billableOrders;
        this.requestRead = requestRead;
    }

    /// 该用户的可开票总览。
    public BillableOverview overview(long userId) {
        Set<Long> occupied = requestRead.occupiedOrderIds(userId);
        List<OrderLine> available = billableOrders.completedOrders(userId).stream()
                .filter(line -> !occupied.contains(line.orderId()))
                .toList();
        BigDecimal total = available.stream().map(OrderLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        return new BillableOverview(available, total, billableOrders.balanceOf(userId));
    }
}
