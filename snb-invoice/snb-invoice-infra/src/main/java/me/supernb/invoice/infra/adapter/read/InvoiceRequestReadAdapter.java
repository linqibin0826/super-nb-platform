package me.supernb.invoice.infra.adapter.read;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.model.read.AdminInvoiceRow;
import me.supernb.invoice.domain.model.read.InvoiceRequestDetail;
import me.supernb.invoice.domain.model.read.InvoiceRequestView;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.model.read.PdfFile;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

/// 发票申请只读投影(主数据源 JdbcTemplate)。列表 SQL 一律不含 bytes 列——PDF 只在 pdfOf 加载。
@Component
public class InvoiceRequestReadAdapter implements InvoiceRequestReadPort {

    private static final RowMapper<InvoiceRequestView> VIEW_MAPPER = (rs, i) -> new InvoiceRequestView(
            rs.getLong("id"), rs.getString("request_no"), rs.getBigDecimal("amount"), rs.getBigDecimal("fee"),
            InvoiceStatus.valueOf(rs.getString("status")), rs.getString("profile_title"), rs.getString("remark"),
            rs.getString("reject_reason"), rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("issued_at") == null ? null : rs.getTimestamp("issued_at").toInstant());

    private final JdbcTemplate jdbc;

    /// 构造:注入主数据源 JdbcTemplate。
    public InvoiceRequestReadAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<InvoiceRequestView> listByUser(long userId) {
        return jdbc.query("SELECT id, request_no, amount, fee, status, profile_title, remark, reject_reason, "
                + "created_at, issued_at FROM invoice.invoice_request WHERE user_id = ? "
                + "ORDER BY created_at DESC, id DESC", VIEW_MAPPER, userId);
    }

    @Override
    public Optional<InvoiceRequestView> findMine(long userId, long requestId) {
        List<InvoiceRequestView> found = jdbc.query("SELECT id, request_no, amount, fee, status, profile_title, "
                + "remark, reject_reason, created_at, issued_at FROM invoice.invoice_request "
                + "WHERE id = ? AND user_id = ?", VIEW_MAPPER, requestId, userId);
        return found.stream().findFirst();
    }

    @Override
    public Set<Long> occupiedOrderIds(long userId) {
        return new HashSet<>(jdbc.query("SELECT o.order_id FROM invoice.invoice_request_order o "
                + "JOIN invoice.invoice_request r ON r.id = o.request_id "
                + "WHERE r.user_id = ? AND o.active", (rs, i) -> rs.getLong(1), userId));
    }

    @Override
    public AdminPage pageByStatus(InvoiceStatus status, int page, int size) {
        String where = status == null ? "" : " WHERE status = ?";
        Object[] countArgs = status == null ? new Object[0] : new Object[] {status.name()};
        Long total = jdbc.queryForObject("SELECT count(*) FROM invoice.invoice_request" + where, Long.class, countArgs);
        Object[] pageArgs = status == null
                ? new Object[] {size, (page - 1) * size}
                : new Object[] {status.name(), size, (page - 1) * size};
        List<AdminInvoiceRow> items = jdbc.query("SELECT id, request_no, user_id, amount, fee, status, created_at "
                        + "FROM invoice.invoice_request" + where + " ORDER BY created_at DESC, id DESC LIMIT ? OFFSET ?",
                (rs, i) -> new AdminInvoiceRow(rs.getLong("id"), rs.getString("request_no"), rs.getLong("user_id"),
                        rs.getBigDecimal("amount"), rs.getBigDecimal("fee"),
                        InvoiceStatus.valueOf(rs.getString("status")), rs.getTimestamp("created_at").toInstant()),
                pageArgs);
        return new AdminPage(items, total == null ? 0 : total);
    }

    @Override
    public Optional<InvoiceRequestDetail> findDetail(long requestId) {
        List<InvoiceRequestDetail> found = jdbc.query("SELECT * FROM invoice.invoice_request WHERE id = ?",
                (rs, i) -> new InvoiceRequestDetail(rs.getLong("id"), rs.getString("request_no"),
                        rs.getLong("user_id"), rs.getBigDecimal("amount"), rs.getBigDecimal("fee"),
                        InvoiceStatus.valueOf(rs.getString("status")),
                        ProfileType.valueOf(rs.getString("profile_type")), rs.getString("profile_title"),
                        rs.getString("profile_tax_no"), rs.getString("profile_reg_address"),
                        rs.getString("profile_reg_phone"), rs.getString("profile_bank_name"),
                        rs.getString("profile_bank_account"), rs.getString("remark"), rs.getString("reject_reason"),
                        rs.getTimestamp("fee_charged_at") == null ? null : rs.getTimestamp("fee_charged_at").toInstant(),
                        rs.getTimestamp("issued_at") == null ? null : rs.getTimestamp("issued_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant(), List.of()),
                requestId);
        return found.stream().findFirst().map(d -> new InvoiceRequestDetail(d.id(), d.requestNo(), d.userId(),
                d.amount(), d.fee(), d.status(), d.profileType(), d.profileTitle(), d.profileTaxNo(),
                d.profileRegAddress(), d.profileRegPhone(), d.profileBankName(), d.profileBankAccount(),
                d.remark(), d.rejectReason(), d.feeChargedAt(), d.issuedAt(), d.createdAt(),
                jdbc.query("SELECT order_id, order_no, amount, completed_at FROM invoice.invoice_request_order "
                                + "WHERE request_id = ? ORDER BY completed_at",
                        (rs, i) -> new OrderLine(rs.getLong("order_id"), rs.getString("order_no"),
                                rs.getBigDecimal("amount"), rs.getTimestamp("completed_at").toInstant()),
                        requestId)));
    }

    @Override
    public Optional<PdfFile> pdfOf(long requestId) {
        List<PdfFile> found = jdbc.query("SELECT filename, bytes FROM invoice.invoice_pdf WHERE request_id = ?",
                (rs, i) -> new PdfFile(rs.getString("filename"), rs.getBytes("bytes")), requestId);
        return found.stream().findFirst();
    }
}
