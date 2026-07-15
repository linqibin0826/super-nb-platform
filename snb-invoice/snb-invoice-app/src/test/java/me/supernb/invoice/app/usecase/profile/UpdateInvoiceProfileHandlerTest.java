package me.supernb.invoice.app.usecase.profile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.profile.command.DeleteInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.command.UpdateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 改/删抬头:归属未命中统一 404;更新沿用同一套字段校验。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class UpdateInvoiceProfileHandlerTest {

    final InvoiceProfileRepository repo = mock(InvoiceProfileRepository.class);
    static final ProfileData OK = new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null);

    @Test
    void updateMissMeansNotFound() {
        when(repo.update(7, 1, OK)).thenReturn(false);
        assertThatThrownBy(() -> new UpdateInvoiceProfileHandler(repo)
                .handle(new UpdateInvoiceProfileCommand(7, 1, OK)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("不存在");
    }

    @Test
    void updateDelegates() {
        when(repo.update(7, 1, OK)).thenReturn(true);
        new UpdateInvoiceProfileHandler(repo).handle(new UpdateInvoiceProfileCommand(7, 1, OK));
        verify(repo).update(7, 1, OK);
    }

    @Test
    void deleteMissMeansNotFound() {
        when(repo.delete(7, 1)).thenReturn(false);
        assertThatThrownBy(() -> new DeleteInvoiceProfileHandler(repo)
                .handle(new DeleteInvoiceProfileCommand(7, 1)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("不存在");
    }
}
