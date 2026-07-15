package me.supernb.sub2api.admin;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/// sub2api 管理员余额扣/退客户端(admin API `POST /users/{id}/balance`)。发票手续费结算唯一
/// 安全通道——不直写库,复用上游四重保障:负余额保护、幂等键(端点+payload,TTL 2h)、
/// 自动清计费缓存、admin_balance 审计记录。notes 必须含业务单号(如 request_no)使幂等键可辨识。
public class Sub2apiAdminBalanceClient {

    /// 上游明确拒绝(HTTP 错误或信封 code≠0),message 携带上游报文(如负余额保护的拒绝理由)。
    /// 连接类故障(RestClientException 非 Response 型)不在此列,原样上抛由调用方决定重试。
    public static class BalanceOperationException extends RuntimeException {
        public BalanceOperationException(String message) {
            super(message);
        }
    }

    private final RestClient restClient;
    private final String adminKey;

    /// 构造:注入指向 `{sub2apiBaseUrl}/api/v1/admin` 的 RestClient 与 admin token。
    public Sub2apiAdminBalanceClient(RestClient restClient, String adminKey) {
        this.restClient = restClient;
        this.adminKey = adminKey;
    }

    /// 扣余额(operation=subtract);amount 必须 >0(上游 binding gt=0)。
    public void subtract(long userId, BigDecimal amount, String notes) {
        execute(userId, amount, "subtract", notes);
    }

    /// 加余额(operation=add);退手续费用。
    public void add(long userId, BigDecimal amount, String notes) {
        execute(userId, amount, "add", notes);
    }

    private void execute(long userId, BigDecimal amount, String operation, String notes) {
        Map<String, Object> body = Map.of("balance", amount, "operation", operation, "notes", notes);
        Map<?, ?> resp;
        try {
            resp = restClient.post()
                    .uri("/users/{id}/balance", userId)
                    .header("x-api-key", adminKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw new BalanceOperationException(
                    "balance " + operation + " HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }
        Object code = resp == null ? null : resp.get("code");
        if (!(code instanceof Number n) || n.intValue() != 0) {
            throw new BalanceOperationException("balance " + operation + " 被拒: "
                    + (resp == null ? "空响应" : String.valueOf(resp.get("message"))));
        }
    }
}
