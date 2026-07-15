package me.supernb.invoice.app.usecase.request.dto;

import java.math.BigDecimal;
import java.util.List;
import me.supernb.invoice.domain.model.read.OrderLine;

/// 可开票总览:未被占用的订单 + 全选合计 + 当前余额(常量另由 adapter 静态给出)。
public record BillableOverview(List<OrderLine> orders, BigDecimal billableTotal, BigDecimal balance) {
}
