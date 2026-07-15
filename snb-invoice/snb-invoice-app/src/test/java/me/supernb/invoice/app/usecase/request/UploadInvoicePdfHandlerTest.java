package me.supernb.invoice.app.usecase.request;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.request.command.UploadInvoicePdfCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.RequestState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 传 PDF:魔数/大小校验;文件名兜底;INVOICING/ISSUED 才许;sha256 由处理器计算。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class UploadInvoicePdfHandlerTest {

    final InvoiceRequestRepository requests = mock(InvoiceRequestRepository.class);
    final UploadInvoicePdfHandler handler = new UploadInvoicePdfHandler(requests);

    static final byte[] PDF = "%PDF-1.4 hello".getBytes();

    static RequestState state(InvoiceStatus status) {
        return new RequestState(9, "INV9", 7, new BigDecimal("55.00"), status);
    }

    @Test
    void acceptsPdfAndComputesSha() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING)));
        when(requests.attachPdfAndIssue(anyLong(), any())).thenReturn(true);
        assertThatCode(() -> handler.handle(new UploadInvoicePdfCommand(9, "发票.pdf", PDF)))
                .doesNotThrowAnyException();
        ArgumentCaptor<InvoiceRequestRepository.PdfData> captor =
                ArgumentCaptor.forClass(InvoiceRequestRepository.PdfData.class);
        verify(requests).attachPdfAndIssue(org.mockito.ArgumentMatchers.eq(9L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().sha256()).hasSize(64);
    }

    @Test
    void blankFilenameFallsBack() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING)));
        when(requests.attachPdfAndIssue(anyLong(), any())).thenReturn(true);
        handler.handle(new UploadInvoicePdfCommand(9, "  ", PDF));
        ArgumentCaptor<InvoiceRequestRepository.PdfData> captor =
                ArgumentCaptor.forClass(InvoiceRequestRepository.PdfData.class);
        verify(requests).attachPdfAndIssue(org.mockito.ArgumentMatchers.eq(9L), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().filename()).isEqualTo("invoice.pdf");
    }

    @Test
    void nonPdfMagicRejected() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING)));
        assertThatThrownBy(() -> handler.handle(new UploadInvoicePdfCommand(9, "a.pdf", "hello".getBytes())))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("PDF");
    }

    @Test
    void oversizeRejected() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.INVOICING)));
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        big[0] = '%';
        big[1] = 'P';
        big[2] = 'D';
        big[3] = 'F';
        assertThatThrownBy(() -> handler.handle(new UploadInvoicePdfCommand(9, "a.pdf", big)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("10MB");
    }

    @Test
    void pendingStateConflicts() {
        when(requests.findState(9)).thenReturn(Optional.of(state(InvoiceStatus.PENDING)));
        assertThatThrownBy(() -> handler.handle(new UploadInvoicePdfCommand(9, "a.pdf", PDF)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("PENDING");
    }
}
