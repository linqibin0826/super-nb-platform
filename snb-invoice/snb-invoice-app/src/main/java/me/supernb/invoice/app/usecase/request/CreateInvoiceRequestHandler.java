package me.supernb.invoice.app.usecase.request;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.supernb.invoice.app.usecase.request.command.CreateInvoiceRequestCommand;
import me.supernb.invoice.app.usecase.request.dto.CreateInvoiceRequestResult;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.FeePolicy;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import me.supernb.invoice.domain.port.read.InvoiceRequestReadPort;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import org.springframework.stereotype.Service;

/// 提交申请用例:抬头快照 → 订单归属核验(RO 口径) → 占用预检 → 合计门槛 → 费额快照 →
/// 余额预检(fee>0 才查;不预扣,TOCTOU 由受理扣费时上游负余额保护兜底) → 落库。
/// 并发权威在两个 partial unique(撞了由仓储映射 409),这里的预检只为友好报错。
@Service
public class CreateInvoiceRequestHandler
        implements CommandHandler<CreateInvoiceRequestCommand, CreateInvoiceRequestResult> {

    private final InvoiceProfileRepository profiles;
    private final InvoiceRequestRepository requests;
    private final BillableOrderReadPort billableOrders;
    private final InvoiceRequestReadPort requestRead;

    /// 构造:注入抬头/申请仓储与两个读端口。
    public CreateInvoiceRequestHandler(InvoiceProfileRepository profiles, InvoiceRequestRepository requests,
            BillableOrderReadPort billableOrders, InvoiceRequestReadPort requestRead) {
        this.profiles = profiles;
        this.requests = requests;
        this.billableOrders = billableOrders;
        this.requestRead = requestRead;
    }

    @Override
    public CreateInvoiceRequestResult handle(CreateInvoiceRequestCommand cmd) {
        if (cmd.orderIds() == null || cmd.orderIds().isEmpty()) {
            throw InvoiceException.invalidInput("未选择订单");
        }
        String remark = cmd.remark() == null ? null : cmd.remark().strip();
        if (remark != null && remark.length() > 200) {
            throw InvoiceException.invalidInput("备注过长");
        }
        var stored = profiles.find(cmd.userId(), cmd.profileId())
                .orElseThrow(() -> InvoiceException.profileNotFound(cmd.profileId()));
        var profile = stored.data();

        Map<Long, OrderLine> mine = billableOrders.completedOrders(cmd.userId()).stream()
                .collect(Collectors.toMap(OrderLine::orderId, Function.identity()));
        Set<Long> occupied = requestRead.occupiedOrderIds(cmd.userId());
        List<OrderLine> picked = new ArrayList<>();
        for (Long orderId : new LinkedHashSet<>(cmd.orderIds())) {
            OrderLine line = mine.get(orderId);
            if (line == null) {
                throw InvoiceException.invalidInput("订单不存在或不可开票: " + orderId);
            }
            if (occupied.contains(orderId)) {
                throw InvoiceException.ordersOccupied();
            }
            picked.add(line);
        }

        BigDecimal total = picked.stream().map(OrderLine::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (!FeePolicy.meetsMinimum(total)) {
            throw InvoiceException.belowMinimum(total);
        }
        BigDecimal fee = FeePolicy.feeFor(total);
        if (fee.signum() > 0) {
            BigDecimal balance = billableOrders.balanceOf(cmd.userId());
            if (balance.compareTo(fee) < 0) {
                throw InvoiceException.insufficientBalance(balance, fee);
            }
        }

        var created = requests.create(new InvoiceRequestRepository.NewRequest(
                cmd.userId(), total, fee, profile, stored.verifiedAt(), remark, picked));
        return new CreateInvoiceRequestResult(String.valueOf(created.id()), created.requestNo(), total, fee);
    }
}
