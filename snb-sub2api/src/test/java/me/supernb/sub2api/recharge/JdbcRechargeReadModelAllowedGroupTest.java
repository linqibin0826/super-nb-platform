package me.supernb.sub2api.recharge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// inAnyAllowedGroup:用户是否被授权了给定分组之一(`user_allowed_groups` 名单口径,
/// 抽奖「中转接入不可抽」排除规则用)——授权即算,不看有没有建 key、不看订阅。
@Testcontainers
class JdbcRechargeReadModelAllowedGroupTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static RechargeReadModel readModel;

    @BeforeAll
    static void setup() {
        DriverManagerDataSource ds =
                new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE user_allowed_groups (user_id BIGINT, group_id BIGINT, created_at TIMESTAMPTZ)");
        // 用户 1:授权了 146(下游专属)与 5;用户 2:只授权了 5;用户 3:无任何授权
        jdbc.update("INSERT INTO user_allowed_groups (user_id, group_id) VALUES (1,146),(1,5),(2,5)");
        readModel = new JdbcRechargeReadModel(jdbc);
    }

    @Test
    void trueWhenUserHoldsAnyOfTheGroups() {
        assertThat(readModel.inAnyAllowedGroup(1L, List.of(146L))).isTrue();
        assertThat(readModel.inAnyAllowedGroup(1L, List.of(999L, 146L))).isTrue();
    }

    @Test
    void falseWhenUserHoldsNoneOfTheGroups() {
        assertThat(readModel.inAnyAllowedGroup(2L, List.of(146L))).isFalse();
        assertThat(readModel.inAnyAllowedGroup(3L, List.of(146L, 5L))).isFalse();
    }

    @Test
    void emptyGroupListNeverMatches() {
        assertThat(readModel.inAnyAllowedGroup(1L, List.of())).isFalse();
    }
}
