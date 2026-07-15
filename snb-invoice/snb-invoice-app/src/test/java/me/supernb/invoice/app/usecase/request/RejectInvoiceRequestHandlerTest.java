package me.supernb.invoice.app.usecase.request;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.request.command.RejectInvoiceRequestCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.RequestState;
import me.supernb.invoice.domain.port.settlement.FeeSettlementPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 驳回:PENDING 直驳零结算;INVOICING 驳回可选退费(先退后转);理由必填;终态拒绝。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class RejectInvoiceRequestHandlerTest {

    final InvoiceRequestRepository requests = mock(InvoiceRequestRepository.class);
    final FeeSettlementPort settlement = mock(FeeSettlementPort.class);
    final RejectInvoiceRequestHandler handler = new RejectInvoiceRequestHandler(requests, settlement);

    static RequestState state(InvoiceStatus status) {
        return new RequestState(9, "INV9", 7, new BigDecimal("55.00"), status);
    }

    @Test
    void pendingRejectSkipsSettlement() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.PENDING)));
        when(requests.reject(9, "资料不符", Set.of(InvoiceStatus.PENDING))).thenReturn(true);
        assertThatCode(() -> handler.handle(new RejectInvoiceRequestCommand(9, "资料不符", true)))
                .doesNotThrowAnyException();
        verify(settlement, never()).refund(anyLong(), any(), anyString());
    }

    @Test
    void invoicingRejectWithRefund() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING)));
        when(requests.reject(9, "无法开具", Set.of(InvoiceStatus.INVOICING))).thenReturn(true);
        handler.handle(new RejectInvoiceRequestCommand(9, "无法开具", true));
        verify(settlement).refund(7, new BigDecimal("55.00"), "发票手续费退还 INV9");
    }

    @Test
    void invoicingRejectWithoutRefund() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING)));
        when(requests.reject(9, "r", Set.of(InvoiceStatus.INVOICING))).thenReturn(true);
        handler.handle(new RejectInvoiceRequestCommand(9, "r", false));
        verify(settlement, never()).refund(anyLong(), any(), anyString());
    }

    @Test
    void blankReasonRejected() {
        assertThatThrownBy(() -> handler.handle(new RejectInvoiceRequestCommand(9, "  ", false)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("理由");
    }

    @Test
    void terminalStateConflicts() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.ISSUED)));
        assertThatThrownBy(() -> handler.handle(new RejectInvoiceRequestCommand(9, "r", false)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("ISSUED");
    }
}
