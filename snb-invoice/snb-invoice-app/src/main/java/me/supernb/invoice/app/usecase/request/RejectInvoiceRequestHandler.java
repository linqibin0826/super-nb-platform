package me.supernb.invoice.app.usecase.request;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.Set;
import me.supernb.invoice.app.usecase.request.command.RejectInvoiceRequestCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.settlement.FeeSettlementPort;
import org.springframework.stereotype.Service;

/// 驳回用例:PENDING 直驳(没扣过费);INVOICING 驳回可选退费——先退后转,退费失败中止可重试
/// (上游幂等键吸收重试;退成转失败的极小窗口同 charge 的人工核对兜底)。
@Service
public class RejectInvoiceRequestHandler implements CommandHandler<RejectInvoiceRequestCommand, Void> {

    private final InvoiceRequestRepository requests;
    private final FeeSettlementPort settlement;

    /// 构造:注入申请仓储与结算端口。
    public RejectInvoiceRequestHandler(InvoiceRequestRepository requests, FeeSettlementPort settlement) {
        this.requests = requests;
        this.settlement = settlement;
    }

    @Override
    public Void handle(RejectInvoiceRequestCommand cmd) {
        String reason = cmd.reason() == null ? "" : cmd.reason().strip();
        if (reason.isEmpty()) {
            throw InvoiceException.invalidInput("驳回理由不能为空");
        }
        if (reason.length() > 200) {
            throw InvoiceException.invalidInput("驳回理由过长");
        }
        var state = requests.findState(cmd.requestId())
                .orElseThrow(() -> InvoiceException.requestNotFound(cmd.requestId()));
        switch (state.status()) {
            case PENDING -> transition(cmd.requestId(), reason, InvoiceStatus.PENDING);
            case INVOICING -> {
                if (cmd.refundFee() && state.fee().signum() > 0) {
                    settlement.refund(state.userId(), state.fee(), "发票手续费退还 " + state.requestNo());
                }
                transition(cmd.requestId(), reason, InvoiceStatus.INVOICING);
            }
            default -> throw InvoiceException.invalidState("PENDING/INVOICING", state.status());
        }
        return null;
    }

    private void transition(long requestId, String reason, InvoiceStatus from) {
        if (!requests.reject(requestId, reason, Set.of(from))) {
            var now = requests.findState(requestId)
                    .orElseThrow(() -> InvoiceException.requestNotFound(requestId));
            throw InvoiceException.invalidState(from.name(), now.status());
        }
    }
}
