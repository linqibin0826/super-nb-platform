package me.supernb.invoice.app.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.profile.command.CreateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;

/// 建抬头:企业必填税号/个人只要姓名;上限 10 条;核验章=名称+税号与缓存官方记录全对才盖
/// (只读缓存,绝不触发付费调用)。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CreateInvoiceProfileHandlerTest {

    final InvoiceProfileRepository repo = mock(InvoiceProfileRepository.class);
    final CompanyRegistryPort registry = mock(CompanyRegistryPort.class);
    final CreateInvoiceProfileHandler handler = new CreateInvoiceProfileHandler(repo, registry);

    static ProfileData company(String title, String taxNo) {
        return new ProfileData(ProfileType.COMPANY, title, taxNo, null, null, null, null);
    }

    static CompanyRecord official(String name, String taxNo) {
        return new CompanyRecord(name, taxNo, "地址", null, null, null);
    }

    @Test
    void createsCompanyProfileAndReturnsStringId() {
        when(repo.countByUser(7)).thenReturn(0);
        when(registry.cached("某某科技")).thenReturn(Optional.empty());
        when(repo.create(7, company("某某科技", "9144030071526726XG"), null)).thenReturn(123L);
        String id = handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", "9144030071526726XG")));
        assertThat(id).isEqualTo("123");
    }

    @Test
    void stampWhenCachedOfficialMatchesNameAndTaxNo() {
        when(repo.countByUser(7)).thenReturn(0);
        when(registry.cached("某某科技")).thenReturn(Optional.of(official("某某科技", "9144030071526726xg"))); // 税号大小写不敏感
        when(repo.create(anyLong(), any(), any())).thenReturn(1L);
        handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", "9144030071526726XG")));
        ArgumentCaptor<Instant> stamp = ArgumentCaptor.forClass(Instant.class);
        verify(repo).create(eq(7L), eq(company("某某科技", "9144030071526726XG")), stamp.capture());
        assertThat(stamp.getValue()).isNotNull();
    }

    @Test
    void noStampWhenTaxNoMismatches() {
        when(repo.countByUser(7)).thenReturn(0);
        when(registry.cached("某某科技")).thenReturn(Optional.of(official("某某科技", "91440300708461136T")));
        when(repo.create(anyLong(), any(), isNull())).thenReturn(1L);
        handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", "9144030071526726XG")));
        verify(repo).create(eq(7L), any(), isNull());
    }

    @Test
    void garbageTaxNoRejectedByFormat() {
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", "fdsa"))))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("格式");
        verify(repo, never()).create(anyLong(), any(), any());
    }

    @Test
    void personalNeverStampedNorLooksUpCache() {
        when(repo.countByUser(7)).thenReturn(0);
        when(repo.create(anyLong(), any(), isNull())).thenReturn(9L);
        ProfileData personal = new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null);
        assertThat(handler.handle(new CreateInvoiceProfileCommand(7, personal))).isEqualTo("9");
        verify(registry, never()).cached(anyString());
    }

    @Test
    void companyRequiresTaxNo() {
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", " "))))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("税号");
        verify(repo, never()).create(anyLong(), any(), any());
    }

    @Test
    void blankTitleRejected() {
        ProfileData bad = new ProfileData(ProfileType.PERSONAL, " ", null, null, null, null, null);
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceProfileCommand(7, bad)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("抬头名称");
    }

    @Test
    void limitTenProfiles() {
        when(repo.countByUser(7)).thenReturn(10);
        assertThatThrownBy(() -> handler.handle(
                new CreateInvoiceProfileCommand(7, company("某某科技", "9144030071526726XG"))))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("上限");
    }
}
