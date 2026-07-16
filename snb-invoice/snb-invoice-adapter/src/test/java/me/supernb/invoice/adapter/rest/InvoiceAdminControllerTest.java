package me.supernb.invoice.adapter.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoiceDetailDto;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoiceItem;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoicePage;
import me.supernb.invoice.app.usecase.admin.query.AdminInvoiceQueryService;
import me.supernb.invoice.app.usecase.request.command.ChargeInvoiceFeeCommand;
import me.supernb.invoice.app.usecase.request.command.RejectInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.command.UploadInvoicePdfCommand;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.model.read.AdminInvoiceRow;
import me.supernb.invoice.domain.model.read.InvoiceRequestDetail;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 管理端契约:role=admin 守卫(user 触发 InvoiceException,同 InvoiceControllerTest 的
/// standalone 无 advice 说明);分页带完整邮箱;multipart 传 PDF 派发命令。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class InvoiceAdminControllerTest {

    final CommandBus commandBus = mock(CommandBus.class);
    final AdminInvoiceQueryService queries = mock(AdminInvoiceQueryService.class);
    final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    MockMvc mvc;

    @BeforeEach
    void setup() {
        when(introspect.introspect("Bearer admin"))
                .thenReturn(Optional.of(new UserProfile(1, "admin", "active")));
        when(introspect.introspect("Bearer u"))
                .thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        mvc = MockMvcBuilders.standaloneSetup(new InvoiceAdminController(commandBus, queries))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void pageIncludesEmailAndStringIds() throws Exception {
        when(queries.page(InvoiceStatus.PENDING, 1, 20)).thenReturn(new AdminInvoicePage(List.of(
                new AdminInvoiceItem(new AdminInvoiceRow(9, "INV9", 7, new BigDecimal("1100.00"),
                        new BigDecimal("55.00"), InvoiceStatus.PENDING, Instant.parse("2026-07-15T00:00:00Z")),
                        "alice@qq.com")), 1));
        mvc.perform(get("/invoice/v1/admin/requests").param("status", "PENDING")
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value("9"))
                .andExpect(jsonPath("$.items[0].email").value("alice@qq.com"));
    }

    @Test
    void detailChargeRejectDispatch() throws Exception {
        when(queries.detail(9)).thenReturn(new AdminInvoiceDetailDto(new InvoiceRequestDetail(
                9, "INV9", 7, new BigDecimal("1100.00"), new BigDecimal("55.00"), InvoiceStatus.PENDING,
                ProfileType.PERSONAL, "张三", null, null, null, null, null, null, "备注", null, null, null,
                Instant.parse("2026-07-15T00:00:00Z"), List.of()), "alice@qq.com"));
        mvc.perform(get("/invoice/v1/admin/requests/9").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNo").value("INV9"))
                .andExpect(jsonPath("$.email").value("alice@qq.com"));

        when(commandBus.handle(new ChargeInvoiceFeeCommand(9))).thenReturn("INVOICING");
        mvc.perform(post("/invoice/v1/admin/requests/9/charge").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVOICING"));

        mvc.perform(post("/invoice/v1/admin/requests/9/reject").header("Authorization", "Bearer admin")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"资料不符\",\"refundFee\":true}"))
                .andExpect(status().isOk());
        verify(commandBus).handle(new RejectInvoiceRequestCommand(9, "资料不符", true));
    }

    @Test
    void pdfUploadDispatchesBytes() throws Exception {
        byte[] pdf = "%PDF-1.4 y".getBytes();
        mvc.perform(multipart("/invoice/v1/admin/requests/9/pdf")
                        .file(new MockMultipartFile("file", "发票.pdf", "application/pdf", pdf))
                        .header("Authorization", "Bearer admin"))
                .andExpect(status().isOk());
        verify(commandBus).handle(new UploadInvoicePdfCommand(9, "发票.pdf", pdf));
    }

    @Test
    void nonAdminThrowsOnEveryEndpoint() {
        assertThatThrownBy(() -> mvc.perform(get("/invoice/v1/admin/requests").header("Authorization", "Bearer u")))
                .hasMessageContaining("管理员");
        assertThatThrownBy(() -> mvc.perform(post("/invoice/v1/admin/requests/9/charge")
                        .header("Authorization", "Bearer u")))
                .hasMessageContaining("管理员");
    }
}
