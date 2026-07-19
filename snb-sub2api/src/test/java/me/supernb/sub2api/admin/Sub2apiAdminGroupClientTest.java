package me.supernb.sub2api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/// 分组列表客户端:GET /groups/all 带鉴权头;只留 subscription_type=subscription 的组
/// (standard 组不能生成订阅兑换码,混进下拉会引导站长踩上游 400);信封 code≠0 与
/// HTTP 错误均抛 GroupListException。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class Sub2apiAdminGroupClientTest {

    @Test
    void listsOnlySubscriptionGroups() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/groups/all"))
                .andExpect(header("x-api-key", "TESTKEY"))
                .andRespond(withSuccess(
                        "{\"code\":0,\"message\":\"success\",\"data\":["
                                + "{\"id\":81,\"name\":\"Claude $50 日卡\",\"subscription_type\":\"subscription\"},"
                                + "{\"id\":5,\"name\":\"标准倍率组\",\"subscription_type\":\"standard\"},"
                                + "{\"id\":90,\"name\":\"GPT $20 日卡\",\"subscription_type\":\"subscription\"}]}",
                        MediaType.APPLICATION_JSON));

        Sub2apiAdminGroupClient client = new Sub2apiAdminGroupClient(builder.build(), "TESTKEY");
        List<GroupSummary> groups = client.listActiveSubscriptionGroups();
        assertThat(groups).containsExactly(
                new GroupSummary(81, "Claude $50 日卡"),
                new GroupSummary(90, "GPT $20 日卡"));
        server.verify();
    }

    @Test
    void httpErrorSurfacesUpstreamMessage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/groups/all"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":401,\"message\":\"invalid admin key\"}"));

        assertThatThrownBy(() -> new Sub2apiAdminGroupClient(builder.build(), "TESTKEY")
                .listActiveSubscriptionGroups())
                .isInstanceOf(Sub2apiAdminGroupClient.GroupListException.class)
                .hasMessageContaining("invalid admin key");
    }

    @Test
    void nonZeroEnvelopeCodeIsRejection() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/groups/all"))
                .andRespond(withSuccess("{\"code\":500,\"message\":\"internal error\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new Sub2apiAdminGroupClient(builder.build(), "TESTKEY")
                .listActiveSubscriptionGroups())
                .isInstanceOf(Sub2apiAdminGroupClient.GroupListException.class)
                .hasMessageContaining("internal error");
    }
}
