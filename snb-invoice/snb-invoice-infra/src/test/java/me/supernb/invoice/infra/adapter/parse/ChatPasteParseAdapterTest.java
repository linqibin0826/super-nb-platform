package me.supernb.invoice.infra.adapter.parse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.sub2api.chat.Sub2apiChatClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.ObjectProvider;

/// AI 识别适配器:JSON 映射/围栏剥离/防幻觉守卫(编造的数字字段置 null)/通道缺席 fail-closed。
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class ChatPasteParseAdapterTest {

    final Sub2apiChatClient client = mock(Sub2apiChatClient.class);

    @SuppressWarnings("unchecked")
    ChatPasteParseAdapter adapterWith(Sub2apiChatClient c) {
        ObjectProvider<Sub2apiChatClient> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(c);
        return new ChatPasteParseAdapter(provider);
    }

    static final String BLOB = "抬头是腾讯科技（深圳）有限公司，税号91440300715 26726XG，电话0755-86013388";

    @Test
    void parsesModelJsonAndUppercasesTaxNo() {
        when(client.complete(anyString(), anyString())).thenReturn(
                "{\"title\":\"腾讯科技（深圳）有限公司\",\"taxNo\":\"9144030071526726xg\","
                        + "\"regAddress\":null,\"regPhone\":\"0755-86013388\",\"bankName\":null,\"bankAccount\":null}");
        var info = adapterWith(client).parse(BLOB).orElseThrow();
        assertThat(info.title()).isEqualTo("腾讯科技（深圳）有限公司");
        assertThat(info.taxNo()).isEqualTo("9144030071526726XG"); // 原文有空格,归一比对后放行并大写
        assertThat(info.regPhone()).isEqualTo("0755-86013388");
        assertThat(info.bankName()).isNull();
    }

    @Test
    void hallucinatedDigitsDroppedToNull() {
        when(client.complete(anyString(), anyString())).thenReturn(
                "{\"title\":\"腾讯科技（深圳）有限公司\",\"taxNo\":\"91440300708461136T\","
                        + "\"regAddress\":null,\"regPhone\":null,\"bankName\":null,\"bankAccount\":\"6222000011112222\"}");
        var info = adapterWith(client).parse(BLOB).orElseThrow(); // 税号/账号都不在原文里
        assertThat(info.taxNo()).isNull();
        assertThat(info.bankAccount()).isNull();
        assertThat(info.title()).isEqualTo("腾讯科技（深圳）有限公司");
    }

    @Test
    void stripsMarkdownFences() {
        when(client.complete(anyString(), anyString())).thenReturn(
                "```json\n{\"title\":\"腾讯科技（深圳）有限公司\",\"taxNo\":null,\"regAddress\":null,"
                        + "\"regPhone\":null,\"bankName\":null,\"bankAccount\":null}\n```");
        assertThat(adapterWith(client).parse(BLOB).orElseThrow().title()).isEqualTo("腾讯科技（深圳）有限公司");
    }

    @Test
    void allNullFieldsMeansEmpty() {
        when(client.complete(anyString(), anyString())).thenReturn(
                "{\"title\":null,\"taxNo\":null,\"regAddress\":null,\"regPhone\":null,\"bankName\":null,\"bankAccount\":null}");
        assertThat(adapterWith(client).parse(BLOB)).isEmpty();
    }

    @Test
    void unparseableOutputIsUnavailable() {
        when(client.complete(anyString(), anyString())).thenReturn("对不起,我无法处理这个请求");
        assertThatThrownBy(() -> adapterWith(client).parse(BLOB))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("AI 识别暂不可用");
    }

    @Test
    void missingClientFailsClosed() {
        assertThatThrownBy(() -> adapterWith(null).parse(BLOB))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("未配置");
    }
}
