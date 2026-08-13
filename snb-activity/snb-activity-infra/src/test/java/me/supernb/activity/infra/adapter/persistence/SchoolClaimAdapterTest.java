package me.supernb.activity.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import me.supernb.activity.domain.model.school.SchoolClaimRecord;
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

/// 开学季领取台账:占位幂等((user_id,kind,tier) 唯一)、状态机回写、按用户捞全量。
/// 判重唯一真源=本表,绝不用订阅 notes 匹配(疯四 alreadyClaimed 教训,runbook ai-relay 36)。
@SpringBootTest(classes = SchoolClaimInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class SchoolClaimAdapterTest {

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

    @Autowired
    SchoolClaimAdapter adapter;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE activity.school_claim");
    }

    @Test
    void insertPendingThenFindRoundTrips() {
        Optional<SchoolClaimRecord> rec =
                adapter.insertPending(42L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100, 130L);
        assertThat(rec).isPresent();
        SchoolClaimRecord found =
                adapter.find(42L, SchoolClaimRecord.KIND_FIRST_CHARGE, 100).orElseThrow();
        assertThat(found.grantStatus()).isEqualTo(SchoolClaimRecord.STATUS_PENDING);
        assertThat(found.groupId()).isEqualTo(130L);
        assertThat(found.attempts()).isZero();
    }

    @Test
    void duplicateInsertReturnsEmpty() {
        assertThat(adapter.insertPending(42L, SchoolClaimRecord.KIND_MILESTONE, 1, 133L)).isPresent();
        assertThat(adapter.insertPending(42L, SchoolClaimRecord.KIND_MILESTONE, 1, 133L)).isEmpty();
    }

    @Test
    void sameUserDifferentTierBothInsertable() {
        adapter.insertPending(42L, SchoolClaimRecord.KIND_MILESTONE, 1, 133L);
        adapter.insertPending(42L, SchoolClaimRecord.KIND_MILESTONE, 3, 134L);
        assertThat(adapter.findByUser(42L)).hasSize(2);
    }

    @Test
    void markSuccessAndMarkFailedDriveStateMachine() {
        long id = adapter.insertPending(7L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50, 129L)
                .orElseThrow().id();
        adapter.markFailed(id, "上游 500");
        SchoolClaimRecord failed = adapter.find(7L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50).orElseThrow();
        assertThat(failed.grantStatus()).isEqualTo(SchoolClaimRecord.STATUS_FAILED);
        assertThat(failed.lastError()).isEqualTo("上游 500");
        assertThat(failed.attempts()).isEqualTo(1);

        adapter.markSuccess(id);
        SchoolClaimRecord ok = adapter.find(7L, SchoolClaimRecord.KIND_FIRST_CHARGE, 50).orElseThrow();
        assertThat(ok.grantStatus()).isEqualTo(SchoolClaimRecord.STATUS_SUCCESS);
    }
}
