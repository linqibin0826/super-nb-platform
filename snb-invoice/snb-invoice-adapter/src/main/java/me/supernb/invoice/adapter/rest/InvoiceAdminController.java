package me.supernb.invoice.adapter.rest;

import dev.linqibin.commons.cqrs.CommandBus;
import java.io.IOException;
import java.io.UncheckedIOException;
import me.supernb.invoice.adapter.rest.request.RejectInput;
import me.supernb.invoice.adapter.rest.response.Responses.AdminDetailResponse;
import me.supernb.invoice.adapter.rest.response.Responses.AdminPageResponse;
import me.supernb.invoice.adapter.rest.response.Responses.AdminRowResponse;
import me.supernb.invoice.adapter.rest.response.Responses.StatusResponse;
import me.supernb.invoice.app.usecase.admin.query.AdminInvoiceQueryService;
import me.supernb.invoice.app.usecase.request.command.ChargeInvoiceFeeCommand;
import me.supernb.invoice.app.usecase.request.command.RejectInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.command.UploadInvoicePdfCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.sub2api.auth.CurrentUser;
import me.supernb.sub2api.auth.UserProfile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/// 发票管理端 REST:列表/详情/扣费受理/驳回/传 PDF/复核下载。每个端点先过 requireAdmin
/// (introspect 的 role=admin,否则 403)——站长用主站 admin 账号经父域 cookie SSO 登录。
@RestController
@RequestMapping("/invoice/v1/admin")
public class InvoiceAdminController {

    private final CommandBus commandBus;
    private final AdminInvoiceQueryService queries;

    /// 构造:读注入管理查询服务,写只注入 CommandBus。
    public InvoiceAdminController(CommandBus commandBus, AdminInvoiceQueryService queries) {
        this.commandBus = commandBus;
        this.queries = queries;
    }

    private static void requireAdmin(UserProfile user) {
        if (!"admin".equals(user.role())) {
            throw InvoiceException.adminRequired();
        }
    }

    /// 申请分页(status 可选,非法值 422;page 钳 ≥1,size 钳 [1,100])。
    @GetMapping("/requests")
    public AdminPageResponse page(@CurrentUser UserProfile user,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        requireAdmin(user);
        InvoiceStatus parsed = null;
        if (status != null && !status.isBlank()) {
            try {
                parsed = InvoiceStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                throw InvoiceException.invalidInput("未知状态: " + status);
            }
        }
        var result = queries.page(parsed, Math.max(1, page), Math.min(100, Math.max(1, size)));
        return new AdminPageResponse(result.items().stream().map(AdminRowResponse::of).toList(), result.total());
    }

    /// 申请详情(含完整邮箱与订单明细)。
    @GetMapping("/requests/{id}")
    public AdminDetailResponse detail(@CurrentUser UserProfile user, @PathVariable long id) {
        requireAdmin(user);
        return AdminDetailResponse.of(queries.detail(id));
    }

    /// 扣手续费并受理(fee=0 直接受理;重复点按幂等返回现状)。
    @PostMapping("/requests/{id}/charge")
    public StatusResponse charge(@CurrentUser UserProfile user, @PathVariable long id) {
        requireAdmin(user);
        return new StatusResponse(commandBus.handle(new ChargeInvoiceFeeCommand(id)));
    }

    /// 驳回(INVOICING 驳回可选退费)。
    @PostMapping("/requests/{id}/reject")
    public void reject(@CurrentUser UserProfile user, @PathVariable long id, @RequestBody RejectInput body) {
        requireAdmin(user);
        commandBus.handle(new RejectInvoiceRequestCommand(id, body.reason(), body.refundFee()));
    }

    /// 上传发票 PDF(multipart 字段名 file;INVOICING→ISSUED,ISSUED 重传覆盖)。
    @PostMapping("/requests/{id}/pdf")
    public void uploadPdf(@CurrentUser UserProfile user, @PathVariable long id,
            @RequestParam("file") MultipartFile file) {
        requireAdmin(user);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        commandBus.handle(new UploadInvoicePdfCommand(id, file.getOriginalFilename(), bytes));
    }

    /// 复核下载(不限状态)。
    @GetMapping("/requests/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@CurrentUser UserProfile user, @PathVariable long id) {
        requireAdmin(user);
        return InvoiceController.pdfResponse(queries.pdf(id));
    }
}
