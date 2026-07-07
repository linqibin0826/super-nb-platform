package me.supernb.sub2api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class Sub2apiIntrospectClientTest {

    @Test
    void returnsActiveUserOn200() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/user/profile"))
                .andExpect(header("Authorization", "Bearer T"))
                .andRespond(withSuccess(
                        "{\"data\":{\"id\":7,\"role\":\"user\",\"status\":\"active\"}}",
                        MediaType.APPLICATION_JSON));

        Optional<UserProfile> p = new Sub2apiIntrospectClient(builder.build(), 30).introspect("Bearer T");

        assertThat(p).isPresent();
        assertThat(p.get().id()).isEqualTo(7L);
        assertThat(p.get().isActiveUser()).isTrue();
        server.verify();
    }

    @Test
    void parsesTopLevelShape() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/user/profile"))
                .andRespond(withSuccess(
                        "{\"id\":9,\"role\":\"admin\",\"status\":\"active\"}", MediaType.APPLICATION_JSON));

        UserProfile p = new Sub2apiIntrospectClient(builder.build(), 30).introspect("Bearer X").orElseThrow();

        assertThat(p.id()).isEqualTo(9L);
        assertThat(p.isActiveUser()).isFalse(); // admin 不是终端用户
    }

    @Test
    void emptyOn401() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://sub2api/api/v1/user/profile"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThat(new Sub2apiIntrospectClient(builder.build(), 30).introspect("Bearer bad")).isEmpty();
    }

    @Test
    void cachesWithinTtl() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://sub2api");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // 只设一次期望;第二次命中缓存 → 不发第二次 HTTP,verify 仍通过
        server.expect(requestTo("http://sub2api/api/v1/user/profile"))
                .andRespond(withSuccess(
                        "{\"data\":{\"id\":1,\"role\":\"user\",\"status\":\"active\"}}",
                        MediaType.APPLICATION_JSON));

        Sub2apiIntrospectClient client = new Sub2apiIntrospectClient(builder.build(), 30);
        assertThat(client.introspect("Bearer same")).isPresent();
        assertThat(client.introspect("Bearer same")).isPresent();
        server.verify();
    }

    @Test
    void blankTokenEmpty() {
        assertThat(new Sub2apiIntrospectClient(RestClient.builder().build(), 30).introspect("  ")).isEmpty();
    }
}
