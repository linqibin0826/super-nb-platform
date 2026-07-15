package me.supernb.invoice.app.usecase.request;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import me.supernb.invoice.app.usecase.request.command.UploadInvoicePdfCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.FeePolicy;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import org.springframework.stereotype.Service;

/// 传 PDF 用例:魔数+大小校验、文件名净化、sha256 计算,委托仓储同事务落库+置 ISSUED。
@Service
public class UploadInvoicePdfHandler implements CommandHandler<UploadInvoicePdfCommand, Void> {

    private static final byte[] PDF_MAGIC = "%PDF".getBytes(StandardCharsets.US_ASCII);

    private final InvoiceRequestRepository requests;

    /// 构造:注入申请仓储端口。
    public UploadInvoicePdfHandler(InvoiceRequestRepository requests) {
        this.requests = requests;
    }

    @Override
    public Void handle(UploadInvoicePdfCommand cmd) {
        var state = requests.findState(cmd.requestId())
                .orElseThrow(() -> InvoiceException.requestNotFound(cmd.requestId()));
        if (state.status() != InvoiceStatus.INVOICING && state.status() != InvoiceStatus.ISSUED) {
            throw InvoiceException.invalidState("INVOICING/ISSUED", state.status());
        }
        byte[] bytes = cmd.bytes();
        if (bytes == null || bytes.length == 0) {
            throw InvoiceException.invalidPdf("文件为空");
        }
        if (bytes.length > FeePolicy.MAX_PDF_BYTES) {
            throw InvoiceException.invalidPdf("超过 10MB 上限");
        }
        if (bytes.length < PDF_MAGIC.length || !startsWithMagic(bytes)) {
            throw InvoiceException.invalidPdf("不是 PDF 文件");
        }
        String filename = cmd.filename() == null ? "" : cmd.filename().strip();
        if (filename.isEmpty()) {
            filename = "invoice.pdf";
        }
        if (filename.length() > 200) {
            filename = filename.substring(filename.length() - 200);
        }
        if (!requests.attachPdfAndIssue(cmd.requestId(),
                new InvoiceRequestRepository.PdfData(filename, bytes, sha256(bytes)))) {
            var now = requests.findState(cmd.requestId())
                    .orElseThrow(() -> InvoiceException.requestNotFound(cmd.requestId()));
            throw InvoiceException.invalidState("INVOICING/ISSUED", now.status());
        }
        return null;
    }

    private static boolean startsWithMagic(byte[] bytes) {
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (bytes[i] != PDF_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 缺 SHA-256", e);
        }
    }
}
