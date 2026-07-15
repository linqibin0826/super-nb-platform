package me.supernb.invoice.app.usecase.request;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.invoice.app.usecase.request.command.ChargeInvoiceFeeCommand;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.settlement.FeeSettlementPort;
import org.springframework.stereotype.Service;

/// 扣费受理用例:先调上游扣费、成功才转 INVOICING(设计稿 §4 顺序)。双击/并发安全:
/// 已 INVOICING/ISSUED 幂等返回;并发双扣由上游幂等键(端点+payload,TTL 2h)吸收;
/// 扣成落库失败的残局=管理员重点一次(TTL 内重放不双扣),TTL 外靠 balance-history 里
/// notes 含 request_no 人工核对——每天几张的量级不造对账机器。
@Service
public class ChargeInvoiceFeeHandler implements CommandHandler<ChargeInvoiceFeeCommand, String> {

    private final InvoiceRequestRepository requests;
    private final FeeSettlementPort settlement;

    /// 构造:注入申请仓储与结算端口。
    public ChargeInvoiceFeeHandler(InvoiceRequestRepository requests, FeeSettlementPort settlement) {
        this.requests = requests;
        this.settlement = settlement;
    }

    @Override
    public String handle(ChargeInvoiceFeeCommand cmd) {
        var state = requests.findState(cmd.requestId())
                .orElseThrow(() -> InvoiceException.requestNotFound(cmd.requestId()));
        if (state.status() == InvoiceStatus.INVOICING || state.status() == InvoiceStatus.ISSUED) {
            return state.status().name();
        }
        if (state.status() != InvoiceStatus.PENDING) {
            throw InvoiceException.invalidState("PENDING", state.status());
        }
        if (state.fee().signum() > 0) {
            settlement.charge(state.userId(), state.fee(), "发票手续费 " + state.requestNo());
        }
        if (!requests.markInvoicing(cmd.requestId())) {
            var now = requests.findState(cmd.requestId())
                    .orElseThrow(() -> InvoiceException.requestNotFound(cmd.requestId()));
            if (now.status() != InvoiceStatus.INVOICING && now.status() != InvoiceStatus.ISSUED) {
                throw InvoiceException.invalidState("PENDING", now.status());
            }
            return now.status().name();
        }
        return InvoiceStatus.INVOICING.name();
    }
}
