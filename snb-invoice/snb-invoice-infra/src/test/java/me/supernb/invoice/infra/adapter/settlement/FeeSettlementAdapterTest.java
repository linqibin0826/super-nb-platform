package me.supernb.invoice.infra.adapter.settlement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.sub2api.admin.Sub2apiAdminBalanceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.ObjectProvider;

/// 结算适配:委托扣/退;上游拒绝翻译成 settlementFailed;客户端缺席(admin-key 未配) fail-closed。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class FeeSettlementAdapterTest {

    @SuppressWarnings("unchecked")
    static ObjectProvider<Sub2apiAdminBalanceClient> providerOf(Sub2apiAdminBalanceClient client) {
        ObjectProvider<Sub2apiAdminBalanceClient> provider = mock(ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(client);
        return provider;
    }

    @Test
    void chargeDelegatesToSubtract() {
        Sub2apiAdminBalanceClient client = mock(Sub2apiAdminBalanceClient.class);
        new FeeSettlementAdapter(providerOf(client)).charge(42, new BigDecimal("55.00"), "发票手续费 INV1");
        verify(client).subtract(42, new BigDecimal("55.00"), "发票手续费 INV1");
    }

    @Test
    void refundDelegatesToAdd() {
        Sub2apiAdminBalanceClient client = mock(Sub2apiAdminBalanceClient.class);
        new FeeSettlementAdapter(providerOf(client)).refund(42, new BigDecimal("55.00"), "发票手续费退还 INV1");
        verify(client).add(42, new BigDecimal("55.00"), "发票手续费退还 INV1");
    }

    @Test
    void upstreamRejectionBecomesSettlementFailed() {
        Sub2apiAdminBalanceClient client = mock(Sub2apiAdminBalanceClient.class);
        doThrow(new Sub2apiAdminBalanceClient.BalanceOperationException("balance cannot be negative"))
                .when(client).subtract(anyLong(), any(), anyString());
        assertThatThrownBy(() -> new FeeSettlementAdapter(providerOf(client))
                .charge(42, new BigDecimal("55.00"), "n"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("cannot be negative");
    }

    @Test
    void missingClientFailsClosed() {
        assertThatThrownBy(() -> new FeeSettlementAdapter(providerOf(null))
                .charge(42, new BigDecimal("1.00"), "n"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("未配置");
        assertThatCode(() -> new FeeSettlementAdapter(providerOf(null))).doesNotThrowAnyException(); // 构造不炸
    }
}
