package me.supernb.invoice.app.usecase.request.query;

import java.util.List;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.read.InvoiceRequestView;
import me.supernb.invoice.domain.model.read.PdfFile;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import org.springframework.stereotype.Service;

/// 我的申请查询:列表 + PDF 下载(仅本人、仅 ISSUED)。
@Service
public class MyInvoiceQueryService {

    private final InvoiceRequestReadPort readPort;

    /// 构造:注入申请只读端口。
    public MyInvoiceQueryService(InvoiceRequestReadPort readPort) {
        this.readPort = readPort;
    }

    /// 我的申请列表(创建时间倒序)。
    public List<InvoiceRequestView> list(long userId) {
        return readPort.listByUser(userId);
    }

    /// 下载我的发票:归属未命中 404;非 ISSUED 409;文件缺失 404。
    public PdfFile myPdf(long userId, long requestId) {
        var view = readPort.findMine(userId, requestId)
                .orElseThrow(() -> InvoiceException.requestNotFound(requestId));
        if (view.status() != InvoiceStatus.ISSUED) {
            throw InvoiceException.invalidState("ISSUED", view.status());
        }
        return readPort.pdfOf(requestId).orElseThrow(() -> InvoiceException.pdfNotFound(requestId));
    }
}
