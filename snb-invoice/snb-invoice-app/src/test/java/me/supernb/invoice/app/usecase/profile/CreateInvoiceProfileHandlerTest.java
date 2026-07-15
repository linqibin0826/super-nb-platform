package me.supernb.invoice.app.usecase.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.profile.command.CreateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 建抬头:企业必填税号/个人只要姓名;上限 10 条;成功返回 id 字符串。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class CreateInvoiceProfileHandlerTest {

    final InvoiceProfileRepository repo = mock(InvoiceProfileRepository.class);
    final CreateInvoiceProfileHandler handler = new CreateInvoiceProfileHandler(repo);

    static ProfileData company(String title, String taxNo) {
        return new ProfileData(ProfileType.COMPANY, title, taxNo, null, null, null, null);
    }

    @Test
    void createsCompanyProfileAndReturnsStringId() {
        when(repo.countByUser(7)).thenReturn(0);
        when(repo.create(7, company("某某科技", "91330100X"))).thenReturn(123L);
        String id = handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", "91330100X")));
        assertThat(id).isEqualTo("123");
    }

    @Test
    void companyRequiresTaxNo() {
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceProfileCommand(7, company("某某科技", " "))))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("税号");
        verify(repo, never()).create(anyLong(), any());
    }

    @Test
    void personalRequiresOnlyTitle() {
        when(repo.countByUser(7)).thenReturn(0);
        when(repo.create(anyLong(), any())).thenReturn(9L);
        ProfileData personal = new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null);
        assertThat(handler.handle(new CreateInvoiceProfileCommand(7, personal))).isEqualTo("9");
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
        assertThatThrownBy(() -> handler.handle(new CreateInvoiceProfileCommand(7, company("t", "x"))))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("上限");
    }
}
