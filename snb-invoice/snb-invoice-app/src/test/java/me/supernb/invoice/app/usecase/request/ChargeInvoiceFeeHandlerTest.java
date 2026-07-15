package me.supernb.invoice.app.usecase.request;

import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.request.command.ChargeInvoiceFeeCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.RequestState;
import me.supernb.invoice.domain.port.settlement.FeeSettlementPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 扣费受理:先扣后转;fee=0 跳过结算;已 INVOICING/ISSUED 幂等返回;守卫未命中按当前态重判。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class ChargeInvoiceFeeHandlerTest {

    final InvoiceRequestRepository requests = mock(InvoiceRequestRepository.class);
    final FeeSettlementPort settlement = mock(FeeSettlementPort.class);
    final ChargeInvoiceFeeHandler handler = new ChargeInvoiceFeeHandler(requests, settlement);

    static RequestState state(InvoiceStatus status, String fee) {
        return new RequestState(9, "INV9", 7, new BigDecimal(fee), status);
    }

    @Test
    void chargesThenMarksInvoicing() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.PENDING, "55.00")));
        when(requests.markInvoicing(9)).thenReturn(true);
        assertThat(handler.handle(new ChargeInvoiceFeeCommand(9))).isEqualTo("INVOICING");
        verify(settlement).charge(7, new BigDecimal("55.00"), "发票手续费 INV9");
    }

    @Test
    void zeroFeeSkipsSettlement() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.PENDING, "0.00")));
        when(requests.markInvoicing(9)).thenReturn(true);
        handler.handle(new ChargeInvoiceFeeCommand(9));
        verify(settlement, never()).charge(anyLong(), any(), anyString());
    }

    @Test
    void alreadyInvoicingIsIdempotent() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING, "55.00")));
        assertThat(handler.handle(new ChargeInvoiceFeeCommand(9))).isEqualTo("INVOICING");
        verify(settlement, never()).charge(anyLong(), any(), anyString());
    }

    @Test
    void rejectedStateConflicts() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.REJECTED, "55.00")));
        assertThatThrownBy(() -> handler.handle(new ChargeInvoiceFeeCommand(9)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("REJECTED");
    }

    @Test
    void settlementRejectionPropagatesAndNoTransition() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.PENDING, "55.00")));
        org.mockito.Mockito.doThrow(InvoiceException.settlementFailed("balance cannot be negative"))
                .when(settlement).charge(anyLong(), any(), anyString());
        assertThatThrownBy(() -> handler.handle(new ChargeInvoiceFeeCommand(9)))
                .isInstanceOf(InvoiceException.class);
        verify(requests, never()).markInvoicing(anyLong());
    }

    @Test
    void guardMissWithConcurrentWinnerIsIdempotent() {
        when(requests.findState(9)).thenReturn(
                Optional.of(state(InvoiceStatus.PENDING, "55.00")),
                Optional.of(state(InvoiceStatus.INVOICING, "55.00")));
        when(requests.markInvoicing(9)).thenReturn(false); // 另一次点击先赢
        assertThat(handler.handle(new ChargeInvoiceFeeCommand(9))).isEqualTo("INVOICING");
    }
}
