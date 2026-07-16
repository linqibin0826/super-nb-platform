package me.supernb.invoice.app.usecase.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.parse.PasteAiParsePort;
import me.supernb.invoice.domain.port.parse.PasteAiParsePort.ParsedInfo;
import me.supernb.invoice.domain.port.read.BillableOrderReadPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/// AI 识别服务:文本长度把门(不烧配额) → 开票资格闸(同核验口径) → 日配额 → 走端口。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class PasteAiParseServiceTest {

    final PasteAiParsePort parser = mock(PasteAiParsePort.class);
    final BillableOrderReadPort billable = mock(BillableOrderReadPort.class);

    static final ParsedInfo INFO = new ParsedInfo("腾讯科技（深圳）有限公司", "9144030071526726XG",
            null, null, null, null);

    void recharged(long userId, String amount) {
        when(billable.completedOrders(userId)).thenReturn(List.of(
                new OrderLine(1, "T1", new BigDecimal(amount), Instant.parse("2026-07-01T00:00:00Z"))));
    }

    @Test
    void delegatesAndStrips() {
        var service = new PasteAiParseService(parser, billable, 20, 200);
        recharged(7, "1500");
        when(parser.parse("抬头是腾讯科技（深圳）有限公司")).thenReturn(Optional.of(INFO));
        assertThat(service.parse(7, "  抬头是腾讯科技（深圳）有限公司  ")).contains(INFO);
    }

    @Test
    void tooShortOrTooLongRejectedWithoutBurningQuota() {
        var service = new PasteAiParseService(parser, billable, 1, 200);
        recharged(7, "1500");
        assertThatThrownBy(() -> service.parse(7, "太短了")).isInstanceOf(InvoiceException.class)
                .hasMessageContaining("长度");
        assertThatThrownBy(() -> service.parse(7, "长".repeat(2001))).isInstanceOf(InvoiceException.class)
                .hasMessageContaining("长度");
        verify(parser, never()).parse(anyString());
        when(parser.parse(anyString())).thenReturn(Optional.empty());
        service.parse(7, "这是一段足够长的开票资料文本"); // 把门失败不占配额
    }

    @Test
    void belowRechargeThresholdRejectedBeforeQuota() {
        var service = new PasteAiParseService(parser, billable, 20, 1); // 全站仅 1 次
        recharged(7, "999.99");
        assertThatThrownBy(() -> service.parse(7, "这是一段足够长的开票资料文本"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("充值");
        verify(parser, never()).parse(anyString());
        recharged(8, "3000");
        when(parser.parse(anyString())).thenReturn(Optional.empty());
        service.parse(8, "合格用户仍可用最后一个全站名额");
    }

    @Test
    void userDailyQuotaEnforced() {
        var service = new PasteAiParseService(parser, billable, 1, 200);
        recharged(7, "1500");
        when(parser.parse(anyString())).thenReturn(Optional.empty());
        service.parse(7, "第一次识别的开票资料文本");
        assertThatThrownBy(() -> service.parse(7, "第二次识别的开票资料文本"))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("次数");
    }
}
