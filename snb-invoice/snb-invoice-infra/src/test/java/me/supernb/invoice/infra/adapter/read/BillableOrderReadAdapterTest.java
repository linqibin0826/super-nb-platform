package me.supernb.invoice.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import me.supernb.sub2api.invoice.InvoiceOrderReadModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 薄委托:上游 OrderRow 映射为域内 OrderLine,余额/邮箱原样透传。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class BillableOrderReadAdapterTest {

    final InvoiceOrderReadModel readModel = mock(InvoiceOrderReadModel.class);
    final BillableOrderReadAdapter adapter = new BillableOrderReadAdapter(readModel);

    @Test
    void mapsOrderRowsToOrderLines() {
        Instant at = Instant.parse("2026-07-01T00:00:00Z");
        when(readModel.completedBalanceOrders(7)).thenReturn(
                List.of(new InvoiceOrderReadModel.OrderRow(11, "T11", new BigDecimal("600"), at)));
        assertThat(adapter.completedOrders(7)).singleElement().satisfies(l -> {
            assertThat(l.orderId()).isEqualTo(11);
            assertThat(l.orderNo()).isEqualTo("T11");
            assertThat(l.amount()).isEqualByComparingTo("600");
            assertThat(l.completedAt()).isEqualTo(at);
        });
    }

    @Test
    void passesThroughBalanceAndEmails() {
        when(readModel.balanceOf(7)).thenReturn(new BigDecimal("88.5"));
        when(readModel.emailsByIds(List.of(7L))).thenReturn(Map.of(7L, "a@b.c"));
        assertThat(adapter.balanceOf(7)).isEqualByComparingTo("88.5");
        assertThat(adapter.emailsByIds(List.of(7L))).containsEntry(7L, "a@b.c");
    }
}
