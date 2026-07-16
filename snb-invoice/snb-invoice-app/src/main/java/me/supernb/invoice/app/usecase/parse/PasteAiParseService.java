package me.supernb.invoice.app.usecase.parse;

import java.util.Optional;
import me.supernb.invoice.app.usecase.support.DailyQuota;
import me.supernb.invoice.app.usecase.support.InvoiceEligibility;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.parse.PasteAiParsePort;
import me.supernb.invoice.domain.port.parse.PasteAiParsePort.ParsedInfo;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/// AI 识别服务:文本长度把门(不烧配额,上限同时封住 prompt 体积) → 开票资格闸 →
/// 日配额守卫 → 走端口。前端只在规则识别吃不下时才会调到这里(级联降级)。
@Service
public class PasteAiParseService {

    private final PasteAiParsePort parser;
    private final BillableOrderReadPort billableOrders;
    private final DailyQuota quota;

    /// 构造:注入解析端口、充值只读端口与两级日配额(`invoice.ai.*`,默认单用户 20/全站 200)。
    public PasteAiParseService(PasteAiParsePort parser, BillableOrderReadPort billableOrders,
            @Value("${invoice.ai.user-daily:20}") int userDaily,
            @Value("${invoice.ai.global-daily:200}") int globalDaily) {
        this.parser = parser;
        this.billableOrders = billableOrders;
        this.quota = new DailyQuota(userDaily, globalDaily);
    }

    /// 识别一段粘贴文本;empty = 模型什么都没提取到。资格/配额/通道异常分别 422/429/422。
    public Optional<ParsedInfo> parse(long userId, String text) {
        String blob = text == null ? "" : text.strip();
        if (blob.length() < 10 || blob.length() > 2000) {
            throw InvoiceException.invalidInput("识别文本长度不合法(10~2000 字符)");
        }
        InvoiceEligibility.requireRecharged(billableOrders, userId);
        quota.consume(userId, InvoiceException::aiParseQuotaExceeded);
        return parser.parse(blob);
    }
}
