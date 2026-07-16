package me.supernb.sub2api.chat;

import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/// sub2api `/v1/chat/completions` 最小客户端(OpenAI 兼容,Bearer 头在 RestClient 默认头里)。
/// 平台自家消费:key 属 admin 自有账号,扣的是自己的余额;temperature 0 + max_tokens 收紧,
/// 用途是结构化提取而非生成。连接类/信封异常统一收敛为 [ChatUnavailableException],
/// 由调用方决定降级姿态(开票场景=提示「AI 识别暂不可用」,规则识别结果照用)。
public class Sub2apiChatClient {

    /// 通道不可用(网络/上游拒绝/空响应),message 带简因。
    public static class ChatUnavailableException extends RuntimeException {
        public ChatUnavailableException(String message) {
            super(message);
        }
    }

    private final RestClient restClient;
    private final String model;

    /// 构造:注入指向 `{sub2apiBaseUrl}/v1` 的 RestClient(带 Bearer 默认头)与模型名。
    public Sub2apiChatClient(RestClient restClient, String model) {
        this.restClient = restClient;
        this.model = model;
    }

    /// 单轮补全:system + user 两条消息,返回首个 choice 的正文。
    public String complete(String systemPrompt, String userText) {
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "max_tokens", 300,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userText)));
        ChatResponse resp;
        try {
            resp = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientException e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new ChatUnavailableException(reason.length() > 120 ? reason.substring(0, 120) : reason);
        }
        if (resp == null || resp.choices() == null || resp.choices().isEmpty()
                || resp.choices().get(0).message() == null || resp.choices().get(0).message().content() == null) {
            throw new ChatUnavailableException("模型返回空响应");
        }
        return resp.choices().get(0).message().content();
    }

    /// OpenAI 兼容响应信封(只取所需字段)。
    record ChatResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    record Message(String content) {
    }
}
