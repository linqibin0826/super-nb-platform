package me.supernb.invoice.infra.adapter.parse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Optional;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.port.parse.PasteAiParsePort;
import me.supernb.sub2api.chat.Sub2apiChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/// [PasteAiParsePort] 实现:prompt 钉死「只输出 JSON、缺席为 null、绝不编造」,再叠两层
/// 防幻觉守卫——①数字类字段(税号/电话/账号)归一后必须逐字出现在原文里,对不上直接置 null
/// (LLM 编的税号比不填危险);②响应剥 markdown 围栏后走 Jackson,解析不动=通道故障。
/// 客户端 ObjectProvider 延迟解析:`SUB2API_INVOICE_AI_KEY` 未配时 Bean 缺席,调用报
/// aiParseUnavailable,前端静默降级回规则识别(实测 luna 单次 ~5s/~400 tokens)。
@Component
public class ChatPasteParseAdapter implements PasteAiParsePort {

    static final String SYSTEM_PROMPT = "你是开票资料提取器。从用户提供的文本中提取开票信息,"
            + "只输出一个 JSON 对象(不要 markdown 代码块、不要任何解释),字段固定为:"
            + "{\"title\":公司名称或抬头,\"taxNo\":税号或统一社会信用代码,\"regAddress\":地址,"
            + "\"regPhone\":电话,\"bankName\":开户银行,\"bankAccount\":银行账号}。"
            + "规则:文本中没提到的字段值为 null;绝不编造、猜测或补全任何值;"
            + "taxNo/regPhone/bankAccount 必须逐字来自原文。";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObjectProvider<Sub2apiChatClient> chat;

    /// 构造:注入 chat 客户端的 ObjectProvider(key 未配时 Bean 缺席,上下文照常起)。
    public ChatPasteParseAdapter(ObjectProvider<Sub2apiChatClient> chat) {
        this.chat = chat;
    }

    @Override
    public Optional<ParsedInfo> parse(String blob) {
        Sub2apiChatClient client = chat.getIfAvailable();
        if (client == null) {
            throw InvoiceException.aiParseUnavailable("AI 识别通道未配置");
        }
        String content;
        try {
            content = client.complete(SYSTEM_PROMPT, blob);
        } catch (Sub2apiChatClient.ChatUnavailableException e) {
            throw InvoiceException.aiParseUnavailable(e.getMessage());
        }
        RawFields raw;
        try {
            raw = MAPPER.readValue(stripFences(content), RawFields.class);
        } catch (Exception e) {
            throw InvoiceException.aiParseUnavailable("模型输出不可解析");
        }
        ParsedInfo info = new ParsedInfo(
                clean(raw.title()),
                upper(guardDigits(raw.taxNo(), blob)),
                clean(raw.regAddress()),
                guardDigits(raw.regPhone(), blob),
                clean(raw.bankName()),
                guardDigits(raw.bankAccount(), blob));
        return info.isEmpty() ? Optional.empty() : Optional.of(info);
    }

    /// 模型响应体(与 SYSTEM_PROMPT 的字段契约一致)。
    record RawFields(String title, String taxNo, String regAddress, String regPhone,
                     String bankName, String bankAccount) {
    }

    /// 防幻觉:数字类值归一(去空格/横线/括号)后必须是原文的子串,否则置 null。
    static String guardDigits(String value, String source) {
        String v = clean(value);
        if (v == null) {
            return null;
        }
        String normValue = normalizeDigits(v);
        if (normValue.isEmpty() || !normalizeDigits(source).contains(normValue)) {
            return null;
        }
        return v;
    }

    /// 剥掉 ```json ... ``` 一类围栏(prompt 已禁,防御性再剥一层)。
    static String stripFences(String content) {
        String s = content == null ? "" : content.strip();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        return s.strip();
    }

    private static String normalizeDigits(String s) {
        // 大小写折叠:模型偶尔小写回税号,原文大写不该被误杀
        return s == null ? "" : s.replaceAll("[\\s\\-()（）]", "").toUpperCase(Locale.ROOT);
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() || "null".equalsIgnoreCase(stripped) ? null : stripped;
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
