package me.supernb.invoice.infra.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.infra.adapter.persistence.dao.InvoicePdfJpaRepository;
import me.supernb.invoice.infra.adapter.persistence.dao.InvoiceRequestJpaRepository;
import me.supernb.invoice.infra.adapter.persistence.dao.InvoiceRequestOrderJpaRepository;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoicePdfEntity;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceRequestEntity;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceRequestOrderEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// [InvoiceRequestRepository] 实现:创建单事务落申请单+占用行,撞两个 partial unique 按索引名
/// 映射领域异常(并发权威在数据库,这里只做语义翻译);转移守卫式 UPDATE;驳回/撤回同事务释放占用。
@Repository
public class InvoiceRequestRepositoryAdapter implements InvoiceRequestRepository {

    private final InvoiceRequestJpaRepository requests;
    private final InvoiceRequestOrderJpaRepository orders;
    private final InvoicePdfJpaRepository pdfs;
    private final TransactionTemplate txTemplate;

    /// 构造:注入三个 DAO,事务管理器包成 TransactionTemplate。
    public InvoiceRequestRepositoryAdapter(InvoiceRequestJpaRepository requests,
            InvoiceRequestOrderJpaRepository orders, InvoicePdfJpaRepository pdfs,
            PlatformTransactionManager txManager) {
        this.requests = requests;
        this.orders = orders;
        this.pdfs = pdfs;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public Created create(NewRequest request) {
        try {
            return txTemplate.execute(status -> {
                InvoiceRequestEntity e = requests.save(new InvoiceRequestEntity(
                        request.userId(), request.amount(), request.fee(), request.profile(),
                        request.profileVerifiedAt(), request.remark()));
                request.orders().forEach(line -> orders.save(new InvoiceRequestOrderEntity(e.getId(), line)));
                // saveAndFlush 语义:占用行唯一索引冲突要在本事务内爆出来,不能拖到提交后
                orders.flush();
                return new Created(e.getId(), e.getRequestNo());
            });
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            if (message.contains("ux_invoice_request_active_per_user")) {
                throw InvoiceException.duplicateActiveRequest();
            }
            if (message.contains("ux_invoice_order_active")) {
                throw InvoiceException.ordersOccupied();
            }
            throw e;
        }
    }

    @Override
    public Optional<RequestState> findState(long requestId) {
        return requests.findById(requestId).map(e -> new RequestState(
                e.getId(), e.getRequestNo(), e.getUserId(), e.getFee(), InvoiceStatus.valueOf(e.getStatus())));
    }

    @Override
    public boolean markInvoicing(long requestId) {
        return Boolean.TRUE.equals(txTemplate.execute(status ->
                requests.markInvoicing(requestId, Instant.now()) > 0));
    }

    @Override
    public boolean reject(long requestId, String reason, Set<InvoiceStatus> from) {
        return Boolean.TRUE.equals(txTemplate.execute(status -> {
            int hit = requests.reject(requestId, reason, from.stream().map(Enum::name).toList());
            if (hit == 0) {
                return false;
            }
            orders.releaseByRequest(requestId);
            return true;
        }));
    }

    @Override
    public boolean cancel(long requestId, long userId) {
        return Boolean.TRUE.equals(txTemplate.execute(status -> {
            int hit = requests.cancel(requestId, userId);
            if (hit == 0) {
                return false;
            }
            orders.releaseByRequest(requestId);
            return true;
        }));
    }

    @Override
    public boolean attachPdfAndIssue(long requestId, PdfData pdf) {
        return Boolean.TRUE.equals(txTemplate.execute(status -> {
            var current = requests.findById(requestId).map(e -> InvoiceStatus.valueOf(e.getStatus()));
            if (current.isEmpty()
                    || (current.get() != InvoiceStatus.INVOICING && current.get() != InvoiceStatus.ISSUED)) {
                return false;
            }
            pdfs.findByRequestId(requestId).ifPresentOrElse(
                    e -> {
                        e.apply(pdf);
                        pdfs.save(e);
                    },
                    () -> pdfs.save(new InvoicePdfEntity(requestId, pdf)));
            if (current.get() == InvoiceStatus.INVOICING) {
                // 守卫:markIssued 命中 0 行 = INVOICING 已被并发(驳回/撤回)改走(current 是本事务
                //   开头读的快照,可能过期)。此时 PDF 不能落库(否则终态单据凭空挂 PDF、账务对不上),
                //   整体回滚 + 返回 false;上层 UploadInvoicePdfHandler 据此抛 invalidState 报错。
                if (requests.markIssued(requestId, Instant.now()) == 0) {
                    status.setRollbackOnly();
                    return false;
                }
            }
            return true;
        }));
    }
}
