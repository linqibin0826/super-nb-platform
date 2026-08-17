package me.supernb.ops.adapter.rest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.linqibin.commons.cqrs.CommandBus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.app.usecase.account.command.CreateOpsAccountCommand;
import me.supernb.ops.app.usecase.query.OpsAccountQueryService;
import me.supernb.ops.app.usecase.query.OpsDashboardQueryService;
import me.supernb.ops.app.usecase.query.OpsSecretQueryService;
import me.supernb.ops.app.usecase.query.OpsSubscriptionQueryService;
import me.supernb.ops.app.usecase.query.view.AccountView;
import me.supernb.ops.app.usecase.query.view.SecretView;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.sub2api.auth.CurrentUserArgumentResolver;
import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.auth.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/// 管理端契约:role=admin 守卫(user 触发 OpsException,standalone 无 advice 直接抛出);
/// 列表响应绝不带密文;secret 端点透传解密结果;非法枚举/日期 422 语义由 OpsException 承载。
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class OpsAdminControllersTest {

    final CommandBus commandBus = mock(CommandBus.class);
    final OpsAccountQueryService accountQueries = mock(OpsAccountQueryService.class);
    final OpsSecretQueryService secretQueries = mock(OpsSecretQueryService.class);
    final OpsSubscriptionQueryService subscriptionQueries = mock(OpsSubscriptionQueryService.class);
    final OpsDashboardQueryService dashboardQueries = mock(OpsDashboardQueryService.class);
    final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);

    MockMvc mvc;

    @BeforeEach
    void setup() {
        when(introspect.introspect("Bearer admin"))
                .thenReturn(Optional.of(new UserProfile(1, "admin", "active")));
        when(introspect.introspect("Bearer u"))
                .thenReturn(Optional.of(new UserProfile(7, "user", "active")));
        mvc = MockMvcBuilders.standaloneSetup(
                        new OpsAccountAdminController(commandBus, accountQueries, secretQueries),
                        new OpsSubscriptionAdminController(commandBus, subscriptionQueries),
                        new OpsDashboardAdminController(dashboardQueries))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void nonAdminIsRejectedOnEveryEntrance() {
        for (String path : List.of("/ops/v1/admin/accounts", "/ops/v1/admin/subscriptions",
                "/ops/v1/admin/dashboard", "/ops/v1/admin/accounts/1/secret")) {
            assertThatThrownBy(() -> mvc.perform(get(path).header("Authorization", "Bearer u")))
                    .hasCauseInstanceOf(OpsException.class).hasMessageContaining("管理员");
        }
    }

    @Test
    void accountListCarriesNoCiphertextOrPlaintext() throws Exception {
        when(accountQueries.listAll()).thenReturn(List.of(new AccountView(9L, "a@gmail.com", "gmail", true,
                null, false, "2024", "US", "林琪斌", AccountStatus.ACTIVE, null, null,
                Instant.parse("2026-08-17T00:00:00Z"))));
        mvc.perform(get("/ops/v1/admin/accounts").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("9"))
                .andExpect(jsonPath("$[0].hasPassword").value(true))
                .andExpect(content().string(not(containsString("password_enc"))))
                .andExpect(content().string(not(containsString("v1:"))));
    }

    @Test
    void createAccountDispatchesCommandAndReturnsStringId() throws Exception {
        when(commandBus.handle(any(CreateOpsAccountCommand.class))).thenReturn("77");
        mvc.perform(post("/ops/v1/admin/accounts").header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@gmail.com\",\"password\":\"p\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("77"));
        ArgumentCaptor<CreateOpsAccountCommand> captor = ArgumentCaptor.forClass(CreateOpsAccountCommand.class);
        verify(commandBus).handle(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().status()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void illegalAccountStatusIsRejected() {
        assertThatThrownBy(() -> mvc.perform(post("/ops/v1/admin/accounts")
                        .header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@gmail.com\",\"status\":\"WHATEVER\"}")))
                .hasCauseInstanceOf(OpsException.class).hasMessageContaining("状态非法");
    }

    @Test
    void illegalSubscriptionDateIsRejected() {
        assertThatThrownBy(() -> mvc.perform(post("/ops/v1/admin/subscriptions")
                        .header("Authorization", "Bearer admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\":\"9\",\"service\":\"CHATGPT\",\"status\":\"ACTIVE\","
                                + "\"nextBillingAt\":\"09/01/2026\"}")))
                .hasCauseInstanceOf(OpsException.class).hasMessageContaining("nextBillingAt");
    }

    @Test
    void secretEndpointRevealsDecryptedValues() throws Exception {
        when(secretQueries.reveal(9L)).thenReturn(new SecretView("plain", null));
        mvc.perform(get("/ops/v1/admin/accounts/9/secret").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("plain"))
                .andExpect(jsonPath("$.recoveryPassword").doesNotExist());
    }
}
