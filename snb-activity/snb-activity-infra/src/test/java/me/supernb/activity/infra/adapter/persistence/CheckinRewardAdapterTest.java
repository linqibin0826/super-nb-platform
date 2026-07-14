package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import me.supernb.activity.domain.model.checkin.CheckinRewardCandidate;
import me.supernb.activity.domain.model.checkin.CheckinRewardView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 补给发放台账:占位幂等、状态机三态回写、按状态取批。
@SpringBootTest(classes = CheckinRewardInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class CheckinRewardAdapterTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/activity");
        r.add("spring.flyway.schemas", () -> "activity");
    }

    static final LocalDate MONTH = LocalDate.of(2026, 7, 1);

    @Autowired
    CheckinRewardAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.checkin_reward_grant");
    }

    @Test
    void claimInsertsPendingRowAndReturnsId() {
        Optional<Long> id = adapter.claim(42, MONTH, "B", 65, "checkin-reward-2026-07");
        assertThat(id).isPresent();
        assertThat(adapter.myGrants(42)).extracting(CheckinRewardView::status).containsExactly("pending");
    }

    @Test
    void claimIsIdempotentPerUserMonth() {
        Optional<Long> first = adapter.claim(42, MONTH, "B", 65, "notes");
        Optional<Long> second = adapter.claim(42, MONTH, "C", 71, "notes");
        assertThat(first).isPresent();
        assertThat(second).isEmpty(); // 本月已占位,不因"想改档"而二次插入
        assertThat(adapter.myGrants(42)).hasSize(1);
        assertThat(adapter.myGrants(42).get(0).tier()).isEqualTo("B");
    }

    @Test
    void markSuccessAndFailedAndDeferredUpdateStatus() {
        long a = adapter.claim(1, MONTH, "A", 27, "n").orElseThrow();
        long b = adapter.claim(2, MONTH, "B", 65, "n").orElseThrow();
        long c = adapter.claim(3, MONTH, "C", 71, "n").orElseThrow();
        adapter.markSuccess(a);
        adapter.markFailed(b, "SUBSCRIPTION_ASSIGN_CONFLICT");
        adapter.markDeferred(c);
        assertThat(adapter.myGrants(1).get(0).status()).isEqualTo("success");
        assertThat(adapter.myGrants(2).get(0).status()).isEqualTo("failed");
        assertThat(adapter.myGrants(3).get(0).status()).isEqualTo("deferred");
    }

    @Test
    void byStatusReturnsOnlyMatchingCandidates() {
        long pendingA = adapter.claim(1, MONTH, "A", 27, "n").orElseThrow();
        long willSucceedB = adapter.claim(2, MONTH, "B", 65, "n").orElseThrow();
        adapter.markSuccess(willSucceedB);
        assertThat(adapter.byStatus("pending")).extracting(CheckinRewardCandidate::grantId)
                .containsExactly(pendingA);
        assertThat(adapter.byStatus("success")).extracting(CheckinRewardCandidate::grantId)
                .containsExactly(willSucceedB);
    }
}
