package me.supernb.sub2api.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 按 Asia/Shanghai 自然日边界查注册用户;跨自然日边界(次日 0 点)不算进当天。
@Testcontainers
class JdbcAccountAgeReadModelTest {

    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcAccountAgeReadModel model;
    static JdbcTemplate jdbc;

    @BeforeAll
    static void setup() {
        PG.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
        jdbc.execute("CREATE TABLE users (id BIGINT PRIMARY KEY, created_at TIMESTAMPTZ)");
        model = new JdbcAccountAgeReadModel(jdbc);
    }

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE users");
    }

    @Test
    void registeredOnReturnsOnlyUsersWithinThatLocalDay() {
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate day = LocalDate.of(2026, 4, 5);
        jdbc.update("INSERT INTO users (id, created_at) VALUES (1, ?)",
                Timestamp.from(day.atTime(10, 0).atZone(zone).toInstant()));
        jdbc.update("INSERT INTO users (id, created_at) VALUES (2, ?)",
                Timestamp.from(day.plusDays(1).atStartOfDay(zone).toInstant())); // 次日 0 点,不算当天
        assertThat(model.registeredOn(day, zone)).containsExactly(1L);
    }
}
