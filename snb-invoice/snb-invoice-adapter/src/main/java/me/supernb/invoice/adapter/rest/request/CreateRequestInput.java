package me.supernb.invoice.adapter.rest.request;

import java.util.List;
import me.supernb.invoice.domain.exception.InvoiceException;

/// 提交申请入参:orderIds/profileId 收字符串(JSON id 契约),这里解析回 long,非法数字 422。
public record CreateRequestInput(List<String> orderIds, String profileId, String remark) {

    public List<Long> orderIdsAsLong() {
        try {
            return orderIds == null ? List.of() : orderIds.stream().map(Long::parseLong).toList();
        } catch (NumberFormatException e) {
            throw InvoiceException.invalidInput("订单 id 不是合法数字");
        }
    }

    public long profileIdAsLong() {
        try {
            return Long.parseLong(profileId == null ? "" : profileId);
        } catch (NumberFormatException e) {
            throw InvoiceException.invalidInput("抬头 id 不是合法数字");
        }
    }
}
