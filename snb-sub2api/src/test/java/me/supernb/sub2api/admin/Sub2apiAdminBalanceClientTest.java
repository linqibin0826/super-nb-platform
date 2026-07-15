package me.supernb.sub2api.admin;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/// 余额扣/退客户端:请求体(balance/operation/notes)与鉴权头正确;信封 code≠0 与 HTTP 错误
/// 均抛 BalanceOperationException(携带上游报文,负余额保护的拒绝要能透给管理员看)。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class Sub2apiAdminBalanceClientTest {

    @Test
    void subtractSendsCorrectRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/users/42/balance"))
                .andExpect(header("x-api-key", "TESTKEY"))
                .andExpect(jsonPath("$.balance").value(55.00))
                .andExpect(jsonPath("$.operation").value("subtract"))
                .andExpect(jsonPath("$.notes").value("发票手续费 INV1001"))
                .andRespond(withSuccess("{\"code\":0,\"message\":\"ok\",\"data\":{}}", MediaType.APPLICATION_JSON));

        Sub2apiAdminBalanceClient client = new Sub2apiAdminBalanceClient(builder.build(), "TESTKEY");
        assertThatCode(() -> client.subtract(42, new BigDecimal("55.00"), "发票手续费 INV1001"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    void addSendsAddOperation() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/users/42/balance"))
                .andExpect(jsonPath("$.operation").value("add"))
                .andRespond(withSuccess("{\"code\":0,\"data\":{}}", MediaType.APPLICATION_JSON));

        new Sub2apiAdminBalanceClient(builder.build(), "TESTKEY")
                .add(42, new BigDecimal("55.00"), "发票手续费退还 INV1001");
        server.verify();
    }

    @Test
    void httpErrorSurfacesUpstreamMessage() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/users/42/balance"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":500,\"message\":\"balance cannot be negative, current balance: 10.00\"}"));

        assertThatThrownBy(() -> new Sub2apiAdminBalanceClient(builder.build(), "TESTKEY")
                .subtract(42, new BigDecimal("55.00"), "发票手续费 INV1001"))
                .isInstanceOf(Sub2apiAdminBalanceClient.BalanceOperationException.class)
                .hasMessageContaining("balance cannot be negative");
    }

    @Test
    void nonZeroEnvelopeCodeIsRejection() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api/api/v1/admin");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/admin/users/42/balance"))
                .andRespond(withSuccess("{\"code\":400,\"message\":\"bad operation\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new Sub2apiAdminBalanceClient(builder.build(), "TESTKEY")
                .subtract(42, new BigDecimal("1.00"), "发票手续费 INVx"))
                .isInstanceOf(Sub2apiAdminBalanceClient.BalanceOperationException.class)
                .hasMessageContaining("bad operation");
    }
}
