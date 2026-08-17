package me.supernb.ops.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;
import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.AccountStatus;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubService;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.model.SubTier;
import me.supernb.ops.domain.port.repository.OpsAccountRepository;
import me.supernb.ops.domain.port.repository.OpsAccountRepository.AccountData;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 两个适配器对真实 Flyway schema 的集成测试:账号全字段 roundtrip;email/账号×服务两处唯一约束
/// 分别映射领域异常;日期/金额/布尔列 roundtrip;计数与按账号列表。
@SpringBootTest(classes = OpsInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class OpsRepositoriesTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/ops");
        r.add("spring.flyway.schemas", () -> "ops");
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    OpsAccountRepository accounts;
    @Autowired
    OpsSubscriptionRepository subscriptions;

    static final AccountData ACCOUNT = new AccountData("a@gmail.com", "gmail", "v1:n:c", "r@x.com",
            "v1:n2:c2", "2024", "US", "林琪斌", AccountStatus.UNVERIFIED, "松哥店铺", "备注");

    static SubscriptionData sub(long accountId, SubService service) {
        return new SubscriptionData(accountId, service, SubTier.PRO, "美区", "mekelove", "4732",
                "204.1.67.78", "iPhone 蜂窝", Instant.parse("2026-08-01T03:00:00Z"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1), new BigDecimal("200.00"),
                SubStatus.BANNED, 98L, "GPT-PRO-菲律宾-1号", Instant.parse("2026-08-13T00:00:00Z"),
                Boolean.TRUE, RefundStatus.APPEALING, new BigDecimal("5.00"), LocalDate.of(2026, 8, 13),
                null, LocalDate.of(2026, 8, 20), "已申诉等回信", "风控审核号");
    }

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM ops.subscription");
        jdbc.update("DELETE FROM ops.account");
    }

    @Test
    void accountCrudRoundtripPreservesAllColumns() {
        long id = accounts.create(ACCOUNT);
        var row = accounts.find(id).orElseThrow();
        assertThat(row.data()).isEqualTo(ACCOUNT);
        assertThat(row.createdAt()).isNotNull();
        var changed = new AccountData("a@gmail.com", "gmail", "v1:n:c", "r@x.com", "v1:n2:c2",
                "2024", "US", "林琪斌", AccountStatus.ACTIVE, "松哥店铺", "改了");
        assertThat(accounts.update(id, changed)).isTrue();
        assertThat(accounts.find(id).orElseThrow().data()).isEqualTo(changed);
        assertThat(accounts.listAll()).hasSize(1);
        assertThat(accounts.delete(id)).isTrue();
        assertThat(accounts.find(id)).isEmpty();
        assertThat(accounts.delete(id)).isFalse();
    }

    @Test
    void duplicateEmailMapsToDomainException() {
        accounts.create(ACCOUNT);
        assertThatThrownBy(() -> accounts.create(ACCOUNT))
                .isInstanceOf(OpsException.class).hasMessageContaining("已存在");
    }

    @Test
    void subscriptionUniquePerAccountService() {
        long id = accounts.create(ACCOUNT);
        subscriptions.create(sub(id, SubService.CHATGPT));
        assertThatThrownBy(() -> subscriptions.create(sub(id, SubService.CHATGPT)))
                .isInstanceOf(OpsException.class).hasMessageContaining("已有");
        subscriptions.create(sub(id, SubService.CLAUDE)); // 不同服务不受限
        assertThat(subscriptions.countByAccount(id)).isEqualTo(2);
    }

    @Test
    void subscriptionRoundtripPreservesDatesAndDecimals() {
        long id = accounts.create(ACCOUNT);
        long subId = subscriptions.create(sub(id, SubService.CHATGPT));
        var row = subscriptions.find(subId).orElseThrow();
        assertThat(row.data()).isEqualTo(sub(id, SubService.CHATGPT));
        var changed = new SubscriptionData(id, SubService.CHATGPT, SubTier.PLUS, "日区", null, null,
                null, null, null, null, null, null, SubStatus.CANCELED, null, null, null, null,
                RefundStatus.NONE, null, null, null, null, null, null);
        assertThat(subscriptions.update(subId, changed)).isTrue();
        assertThat(subscriptions.find(subId).orElseThrow().data()).isEqualTo(changed);
        assertThat(subscriptions.delete(subId)).isTrue();
        assertThat(subscriptions.delete(subId)).isFalse();
    }

    @Test
    void countAndListByAccount() {
        long a = accounts.create(ACCOUNT);
        long b = accounts.create(new AccountData("b@gmail.com", "gmail", null, null, null,
                null, null, null, AccountStatus.ACTIVE, null, null));
        subscriptions.create(sub(a, SubService.CHATGPT));
        subscriptions.create(sub(a, SubService.CLAUDE));
        subscriptions.create(sub(b, SubService.CHATGPT));
        assertThat(subscriptions.countByAccount(a)).isEqualTo(2);
        assertThat(subscriptions.listByAccount(a)).hasSize(2);
        assertThat(subscriptions.listAll()).hasSize(3);
    }
}
