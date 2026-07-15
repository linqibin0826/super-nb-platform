package me.supernb.invoice.app.usecase.request.command;

import dev.linqibin.commons.cqrs.Command;
import java.util.List;
import me.supernb.invoice.app.usecase.request.dto.CreateInvoiceRequestResult;

/// 提交发票申请命令(orderIds 允许重复,处理器去重)。
public record CreateInvoiceRequestCommand(long userId, List<Long> orderIds, long profileId, String remark)
        implements Command<CreateInvoiceRequestResult> {
}
