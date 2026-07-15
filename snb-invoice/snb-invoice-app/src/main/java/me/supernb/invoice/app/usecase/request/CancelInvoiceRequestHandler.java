package me.supernb.invoice.app.usecase.request;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.invoice.app.usecase.request.command.CancelInvoiceRequestCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import org.springframework.stereotype.Service;

/// 撤回用例:守卫式(PENDING+归属)一步到位;未命中再查一次定位原因(404 或 409),不泄露他人单据。
@Service
public class CancelInvoiceRequestHandler implements CommandHandler<CancelInvoiceRequestCommand, Void> {

    private final InvoiceRequestRepository requests;

    /// 构造:注入申请仓储端口。
    public CancelInvoiceRequestHandler(InvoiceRequestRepository requests) {
        this.requests = requests;
    }

    @Override
    public Void handle(CancelInvoiceRequestCommand cmd) {
        if (requests.cancel(cmd.requestId(), cmd.userId())) {
            return null;
        }
        var state = requests.findState(cmd.requestId())
                .orElseThrow(() -> InvoiceException.requestNotFound(cmd.requestId()));
        if (state.userId() != cmd.userId()) {
            throw InvoiceException.requestNotFound(cmd.requestId());
        }
        throw InvoiceException.invalidState("PENDING", state.status());
    }
}
