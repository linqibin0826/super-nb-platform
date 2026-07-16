package me.supernb.invoice.app.usecase.profile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.profile.command.DeleteInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.command.UpdateInvoiceProfileCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.StoredProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// 改/删抬头:归属未命中统一 404;核验章跟内容走——名称/税号没动保留原章(缓存过期不冤枉),
/// 动了按缓存重判(对不上即掉章),防「先核验再偷改」。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class UpdateInvoiceProfileHandlerTest {

    final InvoiceProfileRepository repo = mock(InvoiceProfileRepository.class);
    final CompanyRegistryPort registry = mock(CompanyRegistryPort.class);
    final UpdateInvoiceProfileHandler handler = new UpdateInvoiceProfileHandler(repo, registry);

    static final Instant STAMP = Instant.parse("2026-07-16T00:00:00Z");
    static final ProfileData PERSONAL = new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null);

    static ProfileData company(String title, String taxNo, String bank) {
        return new ProfileData(ProfileType.COMPANY, title, taxNo, null, null, bank, null);
    }

    @Test
    void updateMissMeansNotFound() {
        when(repo.find(7, 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> handler.handle(new UpdateInvoiceProfileCommand(7, 1, PERSONAL)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("不存在");
    }

    @Test
    void updateDelegates() {
        when(repo.find(7, 1)).thenReturn(Optional.of(new StoredProfile(PERSONAL, null)));
        when(repo.update(7, 1, PERSONAL, null)).thenReturn(true);
        handler.handle(new UpdateInvoiceProfileCommand(7, 1, PERSONAL));
        verify(repo).update(7, 1, PERSONAL, null);
    }

    @Test
    void untouchedNameAndTaxNoKeepsStampEvenWithoutCache() {
        var before = company("某某科技", "91X", "旧开户行");
        var after = company("某某科技", "91X", "新开户行"); // 只改章外字段
        when(repo.find(7, 1)).thenReturn(Optional.of(new StoredProfile(before, STAMP)));
        when(repo.update(eq(7L), eq(1L), eq(after), eq(STAMP))).thenReturn(true);
        handler.handle(new UpdateInvoiceProfileCommand(7, 1, after));
        verify(repo).update(7, 1, after, STAMP); // 缓存早过期也保章
    }

    @Test
    void editedTaxNoDropsStampWhenCacheDisagrees() {
        var before = company("某某科技", "91X", null);
        var after = company("某某科技", "改过的税号", null);
        when(repo.find(7, 1)).thenReturn(Optional.of(new StoredProfile(before, STAMP)));
        when(registry.cached("某某科技")).thenReturn(Optional.empty());
        when(repo.update(eq(7L), eq(1L), eq(after), isNull())).thenReturn(true);
        handler.handle(new UpdateInvoiceProfileCommand(7, 1, after));
        verify(repo).update(7, 1, after, null); // 偷改税号 → 掉章
    }

    @Test
    void editedTitleRestampsWhenCacheMatches() {
        var before = company("旧名字", "91X", null);
        var after = company("某某科技", "91X", null);
        when(repo.find(7, 1)).thenReturn(Optional.of(new StoredProfile(before, null)));
        when(registry.cached("某某科技"))
                .thenReturn(Optional.of(new CompanyRecord("某某科技", "91X", null, null, null, null)));
        when(repo.update(eq(7L), eq(1L), eq(after), org.mockito.ArgumentMatchers.notNull())).thenReturn(true);
        handler.handle(new UpdateInvoiceProfileCommand(7, 1, after));
        verify(repo).update(eq(7L), eq(1L), eq(after), org.mockito.ArgumentMatchers.notNull()); // 改对了 → 补章
    }

    @Test
    void deleteMissMeansNotFound() {
        when(repo.delete(7, 1)).thenReturn(false);
        assertThatThrownBy(() -> new DeleteInvoiceProfileHandler(repo)
                .handle(new DeleteInvoiceProfileCommand(7, 1)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("不存在");
    }
}
