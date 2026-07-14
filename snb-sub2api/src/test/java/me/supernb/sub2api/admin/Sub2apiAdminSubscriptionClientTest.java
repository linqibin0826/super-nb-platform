package me.supernb.sub2api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/// bulk-assign 客户端:请求体字段名(snake_case)与鉴权头正确、成功/失败/整批错误均正确解析、
/// 传输层失败向上抛出交由调用方决定重试。
class Sub2apiAdminSubscriptionClientTest {

    @Test
    void bulkAssignSendsCorrectRequestAndParsesCreatedStatus() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/subscriptions/bulk-assign"))
                .andExpect(header("x-api-key", "TESTKEY"))
                .andExpect(jsonPath("$.user_ids[0]").value(42))
                .andExpect(jsonPath("$.group_id").value(65))
                .andExpect(jsonPath("$.validity_days").value(3))
                .andExpect(jsonPath("$.notes").value("checkin-reward-2026-07"))
                .andRespond(withSuccess(
                        "{\"code\":0,\"message\":\"ok\",\"data\":{\"success_count\":1,\"created_count\":1,"
                                + "\"reused_count\":0,\"failed_count\":0,\"statuses\":{\"42\":\"created\"},"
                                + "\"errors\":[]}}",
                        MediaType.APPLICATION_JSON));

        Sub2apiAdminSubscriptionClient client = new Sub2apiAdminSubscriptionClient(builder.build(), "TESTKEY");
        BulkAssignResult result = client.bulkAssign(List.of(42L), 65, 3, "checkin-reward-2026-07");

        assertThat(result.statuses()).containsEntry(42L, "created");
        assertThat(result.errors()).isEmpty();
        server.verify();
    }

    @Test
    void bulkAssignSurfacesPerUserFailureAndBatchErrors() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/subscriptions/bulk-assign"))
                .andRespond(withSuccess(
                        "{\"code\":0,\"data\":{\"statuses\":{\"7\":\"failed\"},"
                                + "\"errors\":[\"SUBSCRIPTION_ASSIGN_CONFLICT: user 7\"]}}",
                        MediaType.APPLICATION_JSON));

        BulkAssignResult result = new Sub2apiAdminSubscriptionClient(builder.build(), "TESTKEY")
                .bulkAssign(List.of(7L), 65, 3, "n");

        assertThat(result.statuses()).containsEntry(7L, "failed");
        assertThat(result.errors()).contains("SUBSCRIPTION_ASSIGN_CONFLICT: user 7");
    }

    @Test
    void transportFailurePropagatesToCaller() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/subscriptions/bulk-assign"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        Sub2apiAdminSubscriptionClient client = new Sub2apiAdminSubscriptionClient(builder.build(), "TESTKEY");
        assertThatThrownBy(() -> client.bulkAssign(List.of(1L), 65, 3, "n"))
                .isInstanceOf(RestClientException.class);
    }
}
