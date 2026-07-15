package me.supernb.invoice.domain.port.read;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.read.AdminInvoiceRow;
import me.supernb.invoice.domain.model.read.InvoiceRequestDetail;
import me.supernb.invoice.domain.model.read.InvoiceRequestView;
import me.supernb.invoice.domain.model.read.PdfFile;

/// 发票申请只读投影(本库)。列表查询绝不加载 PDF bytes。
public interface InvoiceRequestReadPort {

    /// 我的申请(创建时间倒序)。
    List<InvoiceRequestView> listByUser(long userId);

    /// 按归属取单条(下载/撤回入口的所有权校验)。
    Optional<InvoiceRequestView> findMine(long userId, long requestId);

    /// 该用户被有效申请占用的订单 id(可开票列表做差集;权威在唯一索引,这里是展示口径)。
    Set<Long> occupiedOrderIds(long userId);

    /// 管理端分页结果。
    record AdminPage(List<AdminInvoiceRow> items, long total) {
    }

    /// 管理端分页(status=null 查全部;创建时间倒序)。
    AdminPage pageByStatus(InvoiceStatus status, int page, int size);

    /// 管理端详情(含订单明细与抬头快照)。
    Optional<InvoiceRequestDetail> findDetail(long requestId);

    /// 取 PDF(仅此方法触碰 bytes)。
    Optional<PdfFile> pdfOf(long requestId);
}
