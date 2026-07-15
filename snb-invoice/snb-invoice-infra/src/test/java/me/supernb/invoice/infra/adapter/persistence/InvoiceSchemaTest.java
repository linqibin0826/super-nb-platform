package me.supernb.invoice.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// V12 基线的两条数据库级不变量:同用户进行中申请唯一、订单被有效申请占用唯一;
/// 驳回态(REJECTED)不占名额、active=false 不占订单。
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class InvoiceSchemaTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    static JdbcTemplate jdbc;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .locations("classpath:db/migration/invoice")
                .schemas("invoice")
                .createSchemas(true)
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword()));
    }

    static void insertRequest(long id, long userId, String status) {
        jdbc.update("INSERT INTO invoice.invoice_request (id, request_no, user_id, amount, fee, status, "
                + "profile_type, profile_title) VALUES (?,?,?,?,?,?,?,?)",
                id, "INV" + id, userId, new java.math.BigDecimal("1000.00"), new java.math.BigDecimal("50.00"),
                status, "PERSONAL", "张三");
    }

    @Test
    void secondActiveRequestPerUserIsRejectedButRejectedStateFreesSlot() {
        insertRequest(1, 100, "PENDING");
        assertThatThrownBy(() -> insertRequest(2, 100, "INVOICING")).isInstanceOf(DuplicateKeyException.class);
        jdbc.update("UPDATE invoice.invoice_request SET status='REJECTED' WHERE id=1");
        insertRequest(3, 100, "PENDING"); // 释放后可再开
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoice.invoice_request WHERE user_id=100", Long.class))
                .isEqualTo(2L);
    }

    @Test
    void activeOrderOccupationIsUniqueButInactiveIsNot() {
        insertRequest(10, 200, "PENDING");
        insertRequest(11, 201, "PENDING");
        jdbc.update("INSERT INTO invoice.invoice_request_order (id, request_id, order_id, order_no, amount, "
                + "completed_at, active) VALUES (101, 10, 9001, 'T9001', '500.00', now(), true)");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO invoice.invoice_request_order (id, request_id, order_id, "
                + "order_no, amount, completed_at, active) VALUES (102, 11, 9001, 'T9001', '500.00', now(), true)"))
                .isInstanceOf(DuplicateKeyException.class);
        jdbc.update("UPDATE invoice.invoice_request_order SET active=false WHERE id=101");
        jdbc.update("INSERT INTO invoice.invoice_request_order (id, request_id, order_id, order_no, amount, "
                + "completed_at, active) VALUES (103, 11, 9001, 'T9001', '500.00', now(), true)");
    }

    @Test
    void pdfIsOnePerRequest() {
        insertRequest(20, 300, "INVOICING");
        jdbc.update("INSERT INTO invoice.invoice_pdf (id, request_id, bytes, filename, size_bytes, sha256) "
                + "VALUES (201, 20, decode('255044462d312e34','hex'), 'a.pdf', 8, 'x')");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO invoice.invoice_pdf (id, request_id, bytes, filename, "
                + "size_bytes, sha256) VALUES (202, 20, decode('2550','hex'), 'b.pdf', 2, 'y')"))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
