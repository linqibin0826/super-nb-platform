package me.supernb.invoice.app.usecase.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.request.command.CreateInvoiceRequestCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.StoredProfile;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.Created;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.NewRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 提交申请:订单归属核验/去重/合计门槛/费额快照/余额预检/占用预检,全过才落库。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CreateInvoiceRequestHandlerTest {

    final InvoiceProfileRepository profiles = mock(InvoiceProfileRepository.class);
    final InvoiceRequestRepository requests = mock(InvoiceRequestRepository.class);
    final BillableOrderReadPort billable = mock(BillableOrderReadPort.class);
    final InvoiceRequestReadPort requestRead = mock(InvoiceRequestReadPort.class);
    final CreateInvoiceRequestHandler handler =
            new CreateInvoiceRequestHandler(profiles, requests, billable, requestRead);

    static final ProfileData PROFILE = new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null);
    static final Instant AT = Instant.parse("2026-07-01T00:00:00Z");
    static final Instant STAMP = Instant.parse("2026-07-16T00:00:00Z");

    static OrderLine order(long id, String amount) {
        return new OrderLine(id, "T" + id, new BigDecimal(amount), AT);
    }

    void stubHappy() {
        when(profiles.find(7, 55)).thenReturn(Optional.of(new StoredProfile(PROFILE, STAMP)));
        when(billable.completedOrders(7)).thenReturn(List.of(order(1, "600"), order(2, "500"), order(3, "3000")));
        when(requestRead.occupiedOrderIds(7)).thenReturn(Set.of());
        when(billable.balanceOf(7)).thenReturn(new BigDecimal("100"));
        when(requests.create(any())).thenReturn(new Created(9L, "INV9"));
    }

    @Test
    void happyPathSnapshotsFeeAndOrders() {
        stubHappy();
        var result = handler.handle(new CreateInvoiceRequestCommand(7, List.of(1L, 2L), 55, " 七月报销 "));
        assertThat(result.id()).isEqualTo("9");
        assertThat(result.amount()).isEqualByComparingTo("1100.00");
        assertThat(result.fee()).isEqualByComparingTo("55.00");
        ArgumentCaptor<NewRequest> captor = ArgumentCaptor.forClass(NewRequest.class);
        verify(requests).create(captor.capture());
        assertThat(captor.getValue().orders()).hasSize(2);
        assertThat(captor.getValue().remark()).isEqualTo("七月报销");
        assertThat(captor.getValue().profile()).isEqualTo(PROFILE);
        assertThat(captor.getValue().profileVerifiedAt()).isEqualTo(STAMP); // 提交那一刻的章随快照落单
    }

    @Test
    void freeAboveThresholdSkipsBalanceCheck() {
        stubHappy();
        when(billable.balanceOf(7)).thenReturn(BigDecimal.ZERO); // 没余额也行,因为免手续费
        var result = handler.handle(new CreateInvoiceRequestCommand(7, List.of(3L), 55, null));
        assertThat(result.fee()).isEqualByComparingTo("0.00");
    }

    @Test
    void unknownOrderRejected() {
        stubHappy();
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceRequestCommand(7, List.of(1L, 999L), 55, null)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("999");
        verify(requests, never()).create(any());
    }

    @Test
    void occupiedOrderRejectedByPrecheck() {
        stubHappy();
        when(requestRead.occupiedOrderIds(7)).thenReturn(Set.of(1L));
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceRequestCommand(7, List.of(1L, 2L), 55, null)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("占用");
    }

    @Test
    void belowMinimumRejected() {
        stubHappy();
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceRequestCommand(7, List.of(2L), 55, null)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("1000");
    }

    @Test
    void insufficientBalanceRejected() {
        stubHappy();
        when(billable.balanceOf(7)).thenReturn(new BigDecimal("54.99"));
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceRequestCommand(7, List.of(1L, 2L), 55, null)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("手续费");
    }

    @Test
    void missingProfileRejected() {
        when(profiles.find(7, 55)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceRequestCommand(7, List.of(1L), 55, null)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("抬头");
    }

    @Test
    void duplicateOrderIdsCollapsed() {
        stubHappy();
        handler.handle(new CreateInvoiceRequestCommand(7, List.of(1L, 1L, 2L), 55, null));
        ArgumentCaptor<NewRequest> captor = ArgumentCaptor.forClass(NewRequest.class);
        verify(requests).create(captor.capture());
        assertThat(captor.getValue().orders()).hasSize(2);
    }
}
