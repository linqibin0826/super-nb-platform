package me.supernb.sub2api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/// 兑换码生成客户端:请求体(count/type=subscription/group_id/validity_days)、鉴权头、
/// 强制 Idempotency-Key 头都对;信封 code≠0 与 HTTP 错误均抛 RedeemCodeGenerationException。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class Sub2apiAdminRedeemCodeClientTest {

    @Test
    void generateSendsCorrectRequestAndParsesCodes() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/redeem-codes/generate"))
                .andExpect(header("x-api-key", "TESTKEY"))
                .andExpect(header("Idempotency-Key", Matchers.notNullValue()))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.type").value("subscription"))
                .andExpect(jsonPath("$.group_id").value(77))
                .andExpect(jsonPath("$.validity_days").value(1))
                .andRespond(withSuccess(
                        "{\"code\":0,\"message\":\"success\",\"data\":["
                                + "{\"id\":1,\"code\":\"aaa111\",\"type\":\"subscription\"},"
                                + "{\"id\":2,\"code\":\"bbb222\",\"type\":\"subscription\"}]}",
                        MediaType.APPLICATION_JSON));

        Sub2apiAdminRedeemCodeClient client = new Sub2apiAdminRedeemCodeClient(builder.build(), "TESTKEY");
        List<String> codes = client.generateSubscriptionCodes(77, 1, 2);
        assertThat(codes).containsExactly("aaa111", "bbb222");
        server.verify();
    }

    @Test
    void httpErrorSurfacesUpstreamMessage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/redeem-codes/generate"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":400,\"message\":\"group must be subscription type\"}"));

        assertThatThrownBy(() -> new Sub2apiAdminRedeemCodeClient(builder.build(), "TESTKEY")
                .generateSubscriptionCodes(1, 1, 1))
                .isInstanceOf(Sub2apiAdminRedeemCodeClient.RedeemCodeGenerationException.class)
                .hasMessageContaining("group must be subscription type");
    }

    @Test
    void nonZeroEnvelopeCodeIsRejection() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/redeem-codes/generate"))
                .andRespond(withSuccess("{\"code\":500,\"message\":\"internal error\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new Sub2apiAdminRedeemCodeClient(builder.build(), "TESTKEY")
                .generateSubscriptionCodes(1, 1, 1))
                .isInstanceOf(Sub2apiAdminRedeemCodeClient.RedeemCodeGenerationException.class)
                .hasMessageContaining("internal error");
    }
}
