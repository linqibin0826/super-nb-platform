package me.supernb.sub2api.referral;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 全量邀请关系(不分活动窗口):排除站长自号(inviter_id=1)与软删用户。
@Testcontainers
class JdbcReferralReadModelAllInviteesTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcReferralReadModel model;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, deleted_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE user_affiliates (user_id BIGINT PRIMARY KEY, inviter_id BIGINT)");
        model = new JdbcReferralReadModel(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE users, user_affiliates");
    }

    @Test
    void excludesOwnerAccountAndSoftDeletedInvitees() {
        jdbc.update("INSERT INTO users (id, deleted_at) VALUES (10, NULL)");
        jdbc.update("INSERT INTO users (id, deleted_at) VALUES (11, NULL)");
        jdbc.update("INSERT INTO users (id, deleted_at) VALUES (12, now())"); // 软删,不计
        jdbc.update("INSERT INTO user_affiliates (user_id, inviter_id) VALUES (10, 5)");
        jdbc.update("INSERT INTO user_affiliates (user_id, inviter_id) VALUES (11, 1)"); // 站长自号,不计
        jdbc.update("INSERT INTO user_affiliates (user_id, inviter_id) VALUES (12, 5)");
        assertThat(model.allInviteeIdsByInviter()).containsExactly(java.util.Map.entry(5L, List.of(10L)));
    }
}
