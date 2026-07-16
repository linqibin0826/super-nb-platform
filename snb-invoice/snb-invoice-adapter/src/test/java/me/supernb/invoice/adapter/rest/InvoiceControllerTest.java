package me.supernb.invoice.adapter.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.app.usecase.profile.command.CreateInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.command.DeleteInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.command.UpdateInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.parse.PasteAiParseService;
import me.supernb.invoice.app.usecase.profile.query.ProfileQueryService;
import me.supernb.invoice.app.usecase.registry.RegistryLookupService;
import me.supernb.invoice.app.usecase.request.command.CancelInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.command.CreateInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.dto.BillableOverview;
import me.supernb.invoice.app.usecase.request.dto.CreateInvoiceRequestResult;
import me.supernb.invoice.app.usecase.request.query.BillableOrderQueryService;
import me.supernb.invoice.app.usecase.request.query.MyInvoiceQueryService;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.model.read.InvoiceRequestView;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.model.read.PdfFile;
import me.supernb.invoice.domain.model.read.ProfileView;
import me.supernb.invoice.domain.port.registry.CompanyRegistryPort.CompanyRecord;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 用户端契约:鉴权经真解析器(mock introspect);id 字符串化;金额数字;PDF attachment。
/// standalone MockMvc 无 commons 异常映射 advice(依赖链复杂,无先例可挂),错误路径断言
/// 「抛出异常」而非具体状态码——真实状态码语义以 Task 10 全栈 WiringTest 为准。
/// 写端点 mock CommandBus,record equals 即断言派发参数。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class InvoiceControllerTest {

    final CommandBus commandBus = mock(CommandBus.class);
    final ProfileQueryService profileQueries = mock(ProfileQueryService.class);
    final BillableOrderQueryService billableQueries = mock(BillableOrderQueryService.class);
    final MyInvoiceQueryService myQueries = mock(MyInvoiceQueryService.class);
    final RegistryLookupService registryLookup = mock(RegistryLookupService.class);
    final PasteAiParseService pasteAiParse = mock(PasteAiParseService.class);
    final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    MockMvc mvc;

    @BeforeEach
    void setup() {
        when(introspect.introspect("Bearer u")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        mvc = MockMvcBuilders.standaloneSetup(
                        new InvoiceController(commandBus, profileQueries, billableQueries, myQueries,
                                registryLookup, pasteAiParse))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void profilesListSerializesIdAsString() throws Exception {
        when(profileQueries.listByUser(7)).thenReturn(List.of(
                new ProfileView(123, ProfileType.COMPANY, "某某科技", "91X", null, null, null, null,
                        Instant.parse("2026-07-16T00:00:00Z"))));
        mvc.perform(get("/invoice/v1/profiles").header("Authorization", "Bearer u"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("123"))
                .andExpect(jsonPath("$[0].type").value("COMPANY"))
                .andExpect(jsonPath("$[0].verifiedAt").exists());
    }

    @Test
    void registryLookupFoundAndMiss() throws Exception {
        when(registryLookup.lookup(7, "腾讯科技（深圳）有限公司")).thenReturn(Optional.of(new CompanyRecord(
                "腾讯科技（深圳）有限公司", "9144030071526726XG", "深圳市南山区腾讯大厦35层",
                "0755-86013388", "招商银行深圳汉京中心支行", "817281823910001")));
        mvc.perform(post("/invoice/v1/registry/lookup").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"腾讯科技（深圳）有限公司\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.official.taxNo").value("9144030071526726XG"))
                .andExpect(jsonPath("$.official.phone").value("0755-86013388"));

        when(registryLookup.lookup(7, "不存在的公司")).thenReturn(Optional.empty());
        mvc.perform(post("/invoice/v1/registry/lookup").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"不存在的公司\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void pasteAiParseFoundAndMiss() throws Exception {
        when(pasteAiParse.parse(7, "抬头是腾讯科技（深圳）有限公司,税号9144030071526726XG"))
                .thenReturn(Optional.of(new me.supernb.invoice.domain.port.parse.PasteAiParsePort.ParsedInfo(
                        "腾讯科技（深圳）有限公司", "9144030071526726XG", null, null, null, null)));
        mvc.perform(post("/invoice/v1/paste/parse").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"抬头是腾讯科技（深圳）有限公司,税号9144030071526726XG\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(true))
                .andExpect(jsonPath("$.fields.taxNo").value("9144030071526726XG"))
                .andExpect(jsonPath("$.fields.regAddress").doesNotExist());

        when(pasteAiParse.parse(7, "什么都没有的一段废话文本")).thenReturn(Optional.empty());
        mvc.perform(post("/invoice/v1/paste/parse").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"什么都没有的一段废话文本\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.found").value(false));
    }

    @Test
    void createProfileDispatchesCommand() throws Exception {
        when(commandBus.handle(new CreateInvoiceProfileCommand(7,
                new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null)))).thenReturn("9");
        mvc.perform(post("/invoice/v1/profiles").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PERSONAL\",\"title\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("9"));
    }

    @Test
    void updateAndDeleteProfileDispatch() throws Exception {
        mvc.perform(put("/invoice/v1/profiles/5").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"PERSONAL\",\"title\":\"李四\"}"))
                .andExpect(status().isOk());
        verify(commandBus).handle(new UpdateInvoiceProfileCommand(7, 5,
                new ProfileData(ProfileType.PERSONAL, "李四", null, null, null, null, null)));

        mvc.perform(delete("/invoice/v1/profiles/5").header("Authorization", "Bearer u"))
                .andExpect(status().isOk());
        verify(commandBus).handle(new DeleteInvoiceProfileCommand(7, 5));
    }

    @Test
    void ordersOverviewExposesConstants() throws Exception {
        when(billableQueries.overview(7)).thenReturn(new BillableOverview(
                List.of(new OrderLine(11, "T11", new BigDecimal("600.00"), Instant.parse("2026-07-01T00:00:00Z"))),
                new BigDecimal("600.00"), new BigDecimal("88.50")));
        mvc.perform(get("/invoice/v1/orders").header("Authorization", "Bearer u"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].orderId").value("11"))
                .andExpect(jsonPath("$.orders[0].amount").value(600.00))
                .andExpect(jsonPath("$.billableTotal").value(600.00))
                .andExpect(jsonPath("$.balance").value(88.50))
                .andExpect(jsonPath("$.minTotal").value(1000))
                .andExpect(jsonPath("$.freeThreshold").value(3000))
                .andExpect(jsonPath("$.feeRate").value(0.05));
    }

    @Test
    void createRequestParsesStringOrderIds() throws Exception {
        when(commandBus.handle(new CreateInvoiceRequestCommand(7, List.of(11L, 12L), 5, "备注")))
                .thenReturn(new CreateInvoiceRequestResult("9", "INV9",
                        new BigDecimal("1100.00"), new BigDecimal("55.00")));
        mvc.perform(post("/invoice/v1/requests").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[\"11\",\"12\"],\"profileId\":\"5\",\"remark\":\"备注\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNo").value("INV9"))
                .andExpect(jsonPath("$.fee").value(55.00));
    }

    @Test
    void malformedOrderIdThrowsInvoiceException() {
        assertThatThrownBy(() -> mvc.perform(post("/invoice/v1/requests").header("Authorization", "Bearer u")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderIds\":[\"abc\"],\"profileId\":\"5\"}")))
                .hasMessageContaining("合法数字");
    }

    @Test
    void listAndCancelRequests() throws Exception {
        when(myQueries.list(7)).thenReturn(List.of(new InvoiceRequestView(9, "INV9",
                new BigDecimal("1100.00"), new BigDecimal("55.00"), InvoiceStatus.PENDING, "张三",
                null, null, Instant.parse("2026-07-15T00:00:00Z"), null)));
        mvc.perform(get("/invoice/v1/requests").header("Authorization", "Bearer u"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("9"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mvc.perform(post("/invoice/v1/requests/9/cancel").header("Authorization", "Bearer u"))
                .andExpect(status().isOk());
        verify(commandBus).handle(new CancelInvoiceRequestCommand(7, 9));
    }

    @Test
    void pdfDownloadIsAttachment() throws Exception {
        when(myQueries.myPdf(7, 9)).thenReturn(new PdfFile("发票.pdf", "%PDF-1.4 x".getBytes()));
        mvc.perform(get("/invoice/v1/requests/9/pdf").header("Authorization", "Bearer u"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")));
    }

    @Test
    void missingTokenThrowsUnauthorized() {
        when(introspect.introspect(org.mockito.ArgumentMatchers.isNull())).thenReturn(Optional.empty());
        // 参数解析阶段(HandlerMethodArgumentResolver)抛出的异常被 ServletException 包了一层,
        // 与控制器方法体内抛出的异常(原样传播,见另两个 throws 测试)不同——实测确认后钉住根因断言。
        assertThatThrownBy(() -> mvc.perform(get("/invoice/v1/profiles")))
                .hasRootCauseInstanceOf(me.supernb.common.UnauthorizedException.class);
    }
}
