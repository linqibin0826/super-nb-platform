package me.supernb.sub2api.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;

/// sub2api 管理员订阅批量分配客户端(admin API `/subscriptions/bulk-assign`)。
/// 补给资格发放唯一安全通道——不写库、不生成 balance 兑换码,一律走 sub2api 的 HTTP admin 接口
/// (ai-relay `.claude/skills/grant-subscription/references/admin-api.md` 实测契约)。
public class Sub2apiAdminSubscriptionClient {

    private final RestClient restClient;
    private final String adminKey;

    /// 构造:注入指向 `{sub2apiBaseUrl}/api/v1/admin` 的 RestClient 与 admin token。
    public Sub2apiAdminSubscriptionClient(RestClient restClient, String adminKey) {
        this.restClient = restClient;
        this.adminKey = adminKey;
    }

    /// 批量分配订阅:notes 必须固定文案(不含时间戳),否则同 (user_id,group_id) 重跑会因
    /// notes 不一致被判 409 冲突而非幂等 reused。单个用户失败不中断整批,调用方按返回的
    /// statuses/errors 逐个处理;网络/HTTP 层失败直接向上抛出(RestClientException),
    /// 不在本类吞掉——重试策略由调用方(月度结算 job)决定。
    public BulkAssignResult bulkAssign(List<Long> userIds, long groupId, int validityDays, String notes) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_ids", userIds);
        body.put("group_id", groupId);
        body.put("validity_days", validityDays);
        body.put("notes", notes);

        Map<?, ?> resp = restClient.post()
                .uri("/subscriptions/bulk-assign")
                .header("x-api-key", adminKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        if (resp == null) {
            return new BulkAssignResult(Map.of(), List.of());
        }
        Map<?, ?> data = (resp.get("data") instanceof Map<?, ?> d) ? d : Map.of();
        Map<Long, String> statuses = new HashMap<>();
        if (data.get("statuses") instanceof Map<?, ?> s) {
            for (Map.Entry<?, ?> e : s.entrySet()) {
                statuses.put(Long.valueOf(e.getKey().toString()), String.valueOf(e.getValue()));
            }
        }
        List<String> errors = new ArrayList<>();
        if (data.get("errors") instanceof List<?> l) {
            for (Object o : l) {
                errors.add(String.valueOf(o));
            }
        }
        return new BulkAssignResult(statuses, errors);
    }

    /// 单发分配订阅(`POST /subscriptions/assign`,同组已有订阅时续期复用),返回订阅 id
    /// ——包机邀请卡开卡/升档要拿 id 存库供后续 reset-quota 调用。响应体缺 id 视为失败直接抛。
    public long assign(long userId, long groupId, int validityDays, String notes) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("group_id", groupId);
        body.put("validity_days", validityDays);
        body.put("notes", notes);

        Map<?, ?> resp = restClient.post()
                .uri("/subscriptions/assign")
                .header("x-api-key", adminKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        Object id = (resp != null && resp.get("data") instanceof Map<?, ?> d) ? d.get("id") : null;
        if (id == null) {
            throw new IllegalStateException("assign 响应缺订阅 id:userId=" + userId + " groupId=" + groupId);
        }
        return Long.parseLong(id.toString().replaceAll("\\.0+$", ""));
    }

    /// 吊销订阅(`POST /subscriptions/{id}/revoke`)。升档换组后旧卡收走;失败由调用方决定容忍度。
    public void revoke(long subscriptionId) {
        restClient.post()
                .uri("/subscriptions/{id}/revoke", subscriptionId)
                .header("x-api-key", adminKey)
                .retrieve()
                .toBodilessEntity();
    }

    /// 重置订阅额度(`POST /subscriptions/{id}/reset-quota`,三窗全清)——总额卡=卡面回满,
    /// Tibo 时刻的物理按钮(fork subscription_handler.go:228 原生端点)。
    public void resetQuota(long subscriptionId) {
        restClient.post()
                .uri("/subscriptions/{id}/reset-quota", subscriptionId)
                .header("x-api-key", adminKey)
                .body(Map.of("daily", true, "weekly", true, "monthly", true))
                .retrieve()
                .toBodilessEntity();
    }
}
