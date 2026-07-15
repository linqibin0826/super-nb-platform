package me.supernb.invoice.app.usecase.admin.query;

import java.util.List;
import java.util.Map;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoiceDetailDto;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoiceItem;
import me.supernb.invoice.app.usecase.admin.dto.AdminInvoicePage;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.read.AdminInvoiceRow;
import me.supernb.invoice.domain.model.read.PdfFile;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import org.springframework.stereotype.Service;

/// 管理端查询:分页/详情补全完整邮箱(批量取,查无置 "-");admin 任意状态可下载 PDF 复核。
@Service
public class AdminInvoiceQueryService {

    private final InvoiceRequestReadPort readPort;
    private final BillableOrderReadPort billableOrders;

    /// 构造:注入本库读端口与上游读端口(邮箱来源)。
    public AdminInvoiceQueryService(InvoiceRequestReadPort readPort, BillableOrderReadPort billableOrders) {
        this.readPort = readPort;
        this.billableOrders = billableOrders;
    }

    /// 分页(status=null 查全部;page 从 1 起)。
    public AdminInvoicePage page(InvoiceStatus status, int page, int size) {
        var result = readPort.pageByStatus(status, page, size);
        Map<Long, String> emails = billableOrders.emailsByIds(
                result.items().stream().map(AdminInvoiceRow::userId).distinct().toList());
        List<AdminInvoiceItem> items = result.items().stream()
                .map(row -> new AdminInvoiceItem(row, emails.getOrDefault(row.userId(), "-")))
                .toList();
        return new AdminInvoicePage(items, result.total());
    }

    /// 详情(404 语义收在本层)。
    public AdminInvoiceDetailDto detail(long requestId) {
        var detail = readPort.findDetail(requestId)
                .orElseThrow(() -> InvoiceException.requestNotFound(requestId));
        return new AdminInvoiceDetailDto(detail,
                billableOrders.emailsByIds(List.of(detail.userId())).getOrDefault(detail.userId(), "-"));
    }

    /// admin 复核下载(不限状态,文件缺失 404)。
    public PdfFile pdf(long requestId) {
        return readPort.pdfOf(requestId).orElseThrow(() -> InvoiceException.pdfNotFound(requestId));
    }
}
