package me.supernb.sub2api.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 查用户当前余额(可为负,计费透支);软删用户与查无此人一律 0。
@Testcontainers
class JdbcUserBalanceReadModelTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcUserBalanceReadModel model;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, balance NUMERIC(20,8), deleted_at TIMESTAMPTZ)");
        model = new JdbcUserBalanceReadModel(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE users");
    }

    @Test
    void returnsBalanceIncludingNegativeOverdraft() {
        jdbc.update("INSERT INTO users (id, balance) VALUES (1, -8.90271207)");
        jdbc.update("INSERT INTO users (id, balance) VALUES (2, 290.76760953)");
        assertThat(model.balance(1)).isEqualByComparingTo("-8.90271207");
        assertThat(model.balance(2)).isEqualByComparingTo("290.76760953");
    }

    @Test
    void unknownOrSoftDeletedUserYieldsZero() {
        jdbc.update("INSERT INTO users (id, balance, deleted_at) VALUES (3, -5, NOW())");
        assertThat(model.balance(999)).isEqualByComparingTo("0");
        assertThat(model.balance(3)).isEqualByComparingTo("0");
    }
}
