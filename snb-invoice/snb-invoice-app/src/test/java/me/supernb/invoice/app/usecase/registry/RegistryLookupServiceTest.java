package me.supernb.invoice.app.usecase.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 核验查询:名称长度把门(不烧配额) → 单用户日限 → 全站日限 → 走端口。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class RegistryLookupServiceTest {

    final CompanyRegistryPort registry = mock(CompanyRegistryPort.class);

    static final CompanyRecord RECORD = new CompanyRecord("腾讯科技（深圳）有限公司", "9144030071526726XG",
            "深圳市南山区腾讯大厦35层", "0755-86013388", "招商银行深圳汉京中心支行", "817281823910001");

    @Test
    void delegatesAndStripsName() {
        var service = new RegistryLookupService(registry, 10, 40);
        when(registry.lookup("腾讯科技（深圳）有限公司")).thenReturn(Optional.of(RECORD));
        assertThat(service.lookup(7, "  腾讯科技（深圳）有限公司  ")).contains(RECORD);
    }

    @Test
    void tooShortNameRejectedWithoutBurningQuota() {
        var service = new RegistryLookupService(registry, 1, 40); // 单用户仅 1 次
        assertThatThrownBy(() -> service.lookup(7, "腾讯"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("长度");
        verify(registry, never()).lookup(anyString());
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        service.lookup(7, "腾讯科技（深圳）有限公司"); // 把门失败不占配额,这次仍可用
    }

    @Test
    void userDailyQuotaEnforced() {
        var service = new RegistryLookupService(registry, 2, 40);
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
        var service = new RegistryLookupService(registry, 10, 2);
        when(registry.lookup(anyString())).thenReturn(Optional.empty());
        service.lookup(1, "第一次查询的公司");
        service.lookup(2, "第二次查询的公司");
        assertThatThrownBy(() -> service.lookup(3, "全站封顶后谁都不行"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("次数");
    }
}
