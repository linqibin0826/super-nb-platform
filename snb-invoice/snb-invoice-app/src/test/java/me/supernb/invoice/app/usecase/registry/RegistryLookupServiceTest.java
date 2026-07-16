package me.supernb.invoice.app.usecase.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 核验查询:名称长度把门(不烧配额) → 开票资格闸(累计充值 ≥ ¥1000,不合格不占配额)
/// → 单用户日限 → 全站日限 → 走端口。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class RegistryLookupServiceTest {

    final CompanyRegistryPort registry = mock(CompanyRegistryPort.class);
    final BillableOrderReadPort billable = mock(BillableOrderReadPort.class);

    static final CompanyRecord RECORD = new CompanyRecord("腾讯科技（深圳）有限公司", "9144030071526726XG",
            "深圳市南山区腾讯大厦35层", "0755-86013388", "招商银行深圳汉京中心支行", "817281823910001");

    /// 给用户垫一笔指定金额的已完成充值单。
    void recharged(long userId, String amount) {
        when(billable.completedOrders(userId)).thenReturn(List.of(
                new OrderLine(1, "T1", new BigDecimal(amount), Instant.parse("2026-07-01T00:00:00Z"))));
    }

    @Test
    void delegatesAndStripsName() {
        var service = new RegistryLookupService(registry, billable, 10, 40);
        recharged(7, "1500");
        when(registry.lookup("腾讯科技（深圳）有限公司")).thenReturn(Optional.of(RECORD));
        assertThat(service.lookup(7, "  腾讯科技（深圳）有限公司  ")).contains(RECORD);
    }

    @Test
    void tooShortNameRejectedWithoutBurningQuota() {
        var service = new RegistryLookupService(registry, billable, 1, 40); // 单用户仅 1 次
        recharged(7, "1500");
        assertThatThrownBy(() -> service.lookup(7, "腾讯"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("长度");
        verify(registry, never()).lookup(anyString());
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        service.lookup(7, "腾讯科技（深圳）有限公司"); // 把门失败不占配额,这次仍可用
    }

    @Test
    void belowRechargeThresholdRejectedBeforeQuotaAndLookup() {
        var service = new RegistryLookupService(registry, billable, 10, 1); // 全站仅 1 次
        recharged(7, "999.99");
        assertThatThrownBy(() -> service.lookup(7, "某某未达标科技有限公司"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("充值");
        verify(registry, never()).lookup(anyString());
        // 不合格的调用没占掉全站配额:合格用户仍可用最后一个名额
        recharged(8, "3000");
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        service.lookup(8, "某某合格科技有限公司");
    }

    @Test
    void exactlyAtThresholdPasses() {
        var service = new RegistryLookupService(registry, billable, 10, 40);
        recharged(7, "1000.00");
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        assertThat(service.lookup(7, "某某踩线科技有限公司")).isEmpty();
    }

    @Test
    void userDailyQuotaEnforced() {
        var service = new RegistryLookupService(registry, billable, 2, 40);
        recharged(7, "1500");
        recharged(8, "1500");
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        service.lookup(7, "第一次查询的公司");
        service.lookup(7, "第二次查询的公司");
        assertThatThrownBy(() -> service.lookup(7, "第三次查询的公司"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("次数");
        service.lookup(8, "别的用户不受影响"); // 单用户限不殃及他人
        verify(registry, times(3)).lookup(anyString());
    }

    @Test
    void globalDailyQuotaEnforced() {
        var service = new RegistryLookupService(registry, billable, 10, 2);
        recharged(1, "1500");
        recharged(2, "1500");
        recharged(3, "1500");
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        service.lookup(1, "第一次查询的公司");
        service.lookup(2, "第二次查询的公司");
        assertThatThrownBy(() -> service.lookup(3, "全站封顶后谁都不行"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("次数");
    }
}
