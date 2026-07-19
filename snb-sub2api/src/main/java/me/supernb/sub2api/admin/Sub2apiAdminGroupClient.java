package me.supernb.sub2api.admin;

import java.util.List;
import java.util.Map;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/// sub2api 分组列表客户端(admin API `GET /groups/all`,默认只含活跃组)。抽奖后台
/// 「选分组生成兑换码」下拉的数据源——只保留 subscription_type=subscription 的组:
/// standard 组不能生成订阅兑换码,混进下拉会引导站长踩上游 400。
public class Sub2apiAdminGroupClient {

    /// 上游明确拒绝(HTTP 错误或信封 code≠0),message 携带上游报文。
    public static class GroupListException extends RuntimeException {
        public GroupListException(String message) {
            super(message);
        }
    }

    private final RestClient restClient;
    private final String adminKey;

    /// 构造:注入指向 `{sub2apiBaseUrl}/api/v1/admin` 的 RestClient 与 admin token。
    public Sub2apiAdminGroupClient(RestClient restClient, String adminKey) {
        this.restClient = restClient;
        this.adminKey = adminKey;
    }

    /// 列出全部活跃的 subscription 分组(生成订阅兑换码的合法目标),按上游返回顺序。
    public List<GroupSummary> listActiveSubscriptionGroups() {
        Map<?, ?> resp;
        try {
            resp = restClient.get()
                    .uri("/groups/all")
                    .header("x-api-key", adminKey)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw new GroupListException(
                    "groups/all HTTP " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        }
        Object code = resp == null ? null : resp.get("code");
        if (!(code instanceof Number n) || n.intValue() != 0) {
            throw new GroupListException("groups/all 被拒: "
                    + (resp == null ? "空响应" : String.valueOf(resp.get("message"))));
        }
        Object data = resp.get("data");
        if (!(data instanceof List<?> list)) {
            throw new GroupListException("groups/all 响应格式异常: data 不是数组");
        }
        return list.stream()
                .filter(item -> item instanceof Map<?, ?> m && "subscription".equals(m.get("subscription_type")))
                .map(item -> {
                    Map<?, ?> m = (Map<?, ?>) item;
                    if (!(m.get("id") instanceof Number id) || !(m.get("name") instanceof String name)) {
                        throw new GroupListException("groups/all 响应格式异常: 缺少 id/name 字段");
                    }
                    return new GroupSummary(id.longValue(), name);
                })
                .toList();
    }
}
