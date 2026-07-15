package me.supernb.invoice.domain.port.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.read.OrderLine;

/// 发票申请聚合持久化端口。状态转移全部守卫式(WHERE status=期望),返回 false 表示守卫未命中;
/// PDF 与申请 1:1,归本聚合(attachPdfAndIssue 同事务写 PDF + 置 ISSUED)。
public interface InvoiceRequestRepository {

    /// 新申请(amount/fee 已算好,profile 为快照,orders 为订单快照行)。
    record NewRequest(long userId, BigDecimal amount, BigDecimal fee,
                      InvoiceProfileRepository.ProfileData profile, String remark, List<OrderLine> orders) {
    }

    /// 创建结果。
    record Created(long id, String requestNo) {
    }

    /// 插入申请单+订单占用行(单事务)。撞 ux_invoice_request_active_per_user 抛
    /// duplicateActiveRequest、撞 ux_invoice_order_active 抛 ordersOccupied(由实现映射)。
    Created create(NewRequest request);

    /// 状态机操作所需的最小快照。
    record RequestState(long id, String requestNo, long userId, BigDecimal fee, InvoiceStatus status) {
    }

    /// 取状态快照。
    Optional<RequestState> findState(long requestId);

    /// PENDING → INVOICING(记 fee_charged_at=now);true=本次转移成功。
    boolean markInvoicing(long requestId);

    /// (from 集合内状态) → REJECTED + 驳回原因,同事务释放订单占用;true=本次转移成功。
    boolean reject(long requestId, String reason, Set<InvoiceStatus> from);

    /// PENDING + 归属该用户 → CANCELLED,同事务释放订单占用;true=本次转移成功。
    boolean cancel(long requestId, long userId);

    /// 待写入的 PDF。
    record PdfData(String filename, byte[] bytes, String sha256) {
    }

    /// 存 PDF(1:1 upsert,重传覆盖)+ 若当前 INVOICING 则置 ISSUED/issued_at;
    /// 仅 INVOICING/ISSUED 允许,其余返回 false。单事务。
    boolean attachPdfAndIssue(long requestId, PdfData pdf);
}
