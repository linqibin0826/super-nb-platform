package me.supernb.sub2api.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/// sub2api 兑换码批量生成客户端(admin API `POST /redeem-codes/generate`)。抽奖奖品的
/// REDEEM_CODE 唯一安全通道——不直连 sub2api 库,前端摸不到 admin key。⚠️ 该端点服务端要求
/// `Idempotency-Key` 必填(RequireKey: true),否则请求直接被拒;每次逻辑生成动作用一个新
/// UUID,不跨用户点击复用——这正是 raffle runbook 记录过的孤儿兑换码历史 bug 的服务端修复对应点。
/// count 上限 100(调用方在 Handler 层校验),本客户端不做跨请求分片,避免把"部分成功部分
/// 失败"的老问题重新引入。
public class Sub2apiAdminRedeemCodeClient {

    /// 上游明确拒绝(HTTP 错误或信封 code≠0),message 携带上游报文。
    public static class RedeemCodeGenerationException extends RuntimeException {
        public RedeemCodeGenerationException(String message) {
            super(message);
        }
    }

    private final RestClient restClient;
    private final String adminKey;

    /// 构造:注入指向 `{sub2apiBaseUrl}/api/v1/admin` 的 RestClient 与 admin token。
    public Sub2apiAdminRedeemCodeClient(RestClient restClient, String adminKey) {
        this.restClient = restClient;
        this.adminKey = adminKey;
    }

    /// 生成 count 张 subscription 类型兑换码,返回按生成顺序排列的明文码值列表。
    public List<String> generateSubscriptionCodes(long groupId, int validityDays, int count) {
        Map<String, Object> body = Map.of(
                "count", count,
                "type", "subscription",
                "group_id", groupId,
                "validity_days", validityDays);
        Map<?, ?> resp;
        try {
            resp = restClient.post()
                    .uri("/redeem-codes/generate")
                    .header("x-api-key", adminKey)
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw new RedeemCodeGenerationException(
                    "redeem-codes generate HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }
        Object code = resp == null ? null : resp.get("code");
        if (!(code instanceof Number n) || n.intValue() != 0) {
            throw new RedeemCodeGenerationException("redeem-codes generate 被拒: "
                    + (resp == null ? "空响应" : String.valueOf(resp.get("message"))));
        }
        Object data = resp.get("data");
        if (!(data instanceof List<?> list)) {
            throw new RedeemCodeGenerationException("redeem-codes generate 响应格式异常: data 不是数组");
        }
        return list.stream()
                .map(item -> {
                    if (!(item instanceof Map<?, ?> m) || !(m.get("code") instanceof String codeStr)) {
                        throw new RedeemCodeGenerationException("redeem-codes generate 响应格式异常: 缺少 code 字段");
                    }
                    return codeStr;
                })
                .toList();
    }
}
