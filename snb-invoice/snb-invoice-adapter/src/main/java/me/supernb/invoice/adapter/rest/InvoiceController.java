package me.supernb.invoice.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import me.supernb.invoice.adapter.rest.request.CreateRequestInput;
import me.supernb.invoice.adapter.rest.request.PasteParseInput;
import me.supernb.invoice.adapter.rest.request.ProfileInput;
import me.supernb.invoice.adapter.rest.request.RegistryLookupInput;
import me.supernb.invoice.adapter.rest.response.Responses.IdResponse;
import me.supernb.invoice.adapter.rest.response.Responses.OrdersOverviewResponse;
import me.supernb.invoice.adapter.rest.response.Responses.PasteParseResponse;
import me.supernb.invoice.adapter.rest.response.Responses.ProfileResponse;
import me.supernb.invoice.adapter.rest.response.Responses.RegistryLookupResponse;
import me.supernb.invoice.adapter.rest.response.Responses.RequestResponse;
import me.supernb.invoice.app.usecase.parse.PasteAiParseService;
import me.supernb.invoice.app.usecase.profile.command.CreateInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.command.DeleteInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.command.UpdateInvoiceProfileCommand;
import me.supernb.invoice.app.usecase.profile.query.ProfileQueryService;
import me.supernb.invoice.app.usecase.registry.RegistryLookupService;
import me.supernb.invoice.app.usecase.request.command.CancelInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.command.CreateInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.dto.CreateInvoiceRequestResult;
import me.supernb.invoice.app.usecase.request.query.BillableOrderQueryService;
import me.supernb.invoice.app.usecase.request.query.MyInvoiceQueryService;
import me.supernb.invoice.domain.model.read.PdfFile;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// 发票用户端 REST:抬头 CRUD / 可开票总览 / 申请提交撤回 / PDF 下载。全端点要求登录
/// (@CurrentUser → introspect),数据一律按当前用户隔离。
@RestController
@RequestMapping("/invoice/v1")
public class InvoiceController {

    private final CommandBus commandBus;
    private final ProfileQueryService profileQueries;
    private final BillableOrderQueryService billableQueries;
    private final MyInvoiceQueryService myQueries;
    private final RegistryLookupService registryLookup;
    private final PasteAiParseService pasteAiParse;

    /// 构造:读注入查询服务,写只注入 CommandBus。
    public InvoiceController(CommandBus commandBus, ProfileQueryService profileQueries,
            BillableOrderQueryService billableQueries, MyInvoiceQueryService myQueries,
            RegistryLookupService registryLookup, PasteAiParseService pasteAiParse) {
        this.commandBus = commandBus;
        this.profileQueries = profileQueries;
        this.billableQueries = billableQueries;
        this.myQueries = myQueries;
        this.registryLookup = registryLookup;
        this.pasteAiParse = pasteAiParse;
    }

    /// 我的抬头列表。
    @GetMapping("/profiles")
    public List<ProfileResponse> profiles(@CurrentUser UserProfile user) {
        return profileQueries.listByUser(user.id()).stream().map(ProfileResponse::of).toList();
    }

    /// 建抬头。
    @PostMapping("/profiles")
    public IdResponse createProfile(@CurrentUser UserProfile user, @RequestBody ProfileInput body) {
        return new IdResponse(commandBus.handle(new CreateInvoiceProfileCommand(user.id(), body.toData())));
    }

    /// 改抬头(全量覆盖)。
    @PutMapping("/profiles/{id}")
    public void updateProfile(@CurrentUser UserProfile user, @PathVariable long id, @RequestBody ProfileInput body) {
        commandBus.handle(new UpdateInvoiceProfileCommand(user.id(), id, body.toData()));
    }

    /// 删抬头。
    @DeleteMapping("/profiles/{id}")
    public void deleteProfile(@CurrentUser UserProfile user, @PathVariable long id) {
        commandBus.handle(new DeleteInvoiceProfileCommand(user.id(), id));
    }

    /// 抬头核验:按企业全称查官方开票资料(付费第三方,双层日配额;found=false=查无)。
    @PostMapping("/registry/lookup")
    public RegistryLookupResponse registryLookup(@CurrentUser UserProfile user,
            @RequestBody RegistryLookupInput body) {
        return RegistryLookupResponse.of(registryLookup.lookup(user.id(), body.name()));
    }

    /// 开票资料 AI 识别:整段粘贴文本 → 抬头字段(自家中转 LLM,规则识别吃不下时的级联兜底;
    /// found=false=模型什么都没提取到)。
    @PostMapping("/paste/parse")
    public PasteParseResponse pasteParse(@CurrentUser UserProfile user, @RequestBody PasteParseInput body) {
        return PasteParseResponse.of(pasteAiParse.parse(user.id(), body.text()));
    }

    /// 可开票总览(未占用订单+合计+余额+业务常量)。
    @GetMapping("/orders")
    public OrdersOverviewResponse orders(@CurrentUser UserProfile user) {
        var overview = billableQueries.overview(user.id());
        return OrdersOverviewResponse.of(overview.orders(), overview.billableTotal(), overview.balance());
    }

    /// 提交申请。
    @PostMapping("/requests")
    public CreateInvoiceRequestResult createRequest(@CurrentUser UserProfile user,
            @RequestBody CreateRequestInput body) {
        return commandBus.handle(new CreateInvoiceRequestCommand(
                user.id(), body.orderIdsAsLong(), body.profileIdAsLong(), body.remark()));
    }

    /// 我的申请列表。
    @GetMapping("/requests")
    public List<RequestResponse> requests(@CurrentUser UserProfile user) {
        return myQueries.list(user.id()).stream().map(RequestResponse::of).toList();
    }

    /// 撤回(仅 PENDING)。
    @PostMapping("/requests/{id}/cancel")
    public void cancel(@CurrentUser UserProfile user, @PathVariable long id) {
        commandBus.handle(new CancelInvoiceRequestCommand(user.id(), id));
    }

    /// 下载我的发票(仅 ISSUED)。文件下载是 ResponseEntity 的合法使用场景(非错误体拼装)。
    @GetMapping("/requests/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@CurrentUser UserProfile user, @PathVariable long id) {
        PdfFile file = myQueries.myPdf(user.id(), id);
        return pdfResponse(file);
    }

    /// PDF 附件响应(admin 控制器复用)。
    static ResponseEntity<byte[]> pdfResponse(PdfFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8).build().toString())
                .body(file.bytes());
    }
}
