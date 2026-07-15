package me.supernb.invoice.app.usecase.request.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 可开票总览:上游订单 - 本库占用 = 可勾选;合计与余额一并给前端。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class BillableOrderQueryServiceTest {

    final BillableOrderReadPort billable = mock(BillableOrderReadPort.class);
    final InvoiceRequestReadPort requestRead = mock(InvoiceRequestReadPort.class);
    final BillableOrderQueryService service = new BillableOrderQueryService(billable, requestRead);

    @Test
    void subtractsOccupiedAndSums() {
        Instant at = Instant.parse("2026-07-01T00:00:00Z");
        when(billable.completedOrders(7)).thenReturn(List.of(
                new OrderLine(1, "T1", new BigDecimal("600"), at),
                new OrderLine(2, "T2", new BigDecimal("500"), at),
                new OrderLine(3, "T3", new BigDecimal("400"), at)));
        when(requestRead.occupiedOrderIds(7)).thenReturn(Set.of(2L));
        when(billable.balanceOf(7)).thenReturn(new BigDecimal("88.5"));

        var overview = service.overview(7);
        assertThat(overview.orders()).extracting(OrderLine::orderId).containsExactly(1L, 3L);
        assertThat(overview.billableTotal()).isEqualByComparingTo("1000.00");
        assertThat(overview.balance()).isEqualByComparingTo("88.5");
    }
}
