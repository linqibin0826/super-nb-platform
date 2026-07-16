package me.supernb.invoice.infra.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.exception.InvoiceException;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.NewRequest;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.PdfData;
import me.supernb.invoice.domain.model.ProfileType;
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

/// 两个写适配器对真实 Flyway schema 的集成测试:抬头归属隔离;创建申请撞两个 partial unique
/// 分别映射领域异常;守卫转移的成功/未命中;驳回/撤回释放订单占用;PDF upsert+ISSUED 回读一致。
@SpringBootTest(classes = InvoiceInfraTestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class InvoiceRepositoriesTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/invoice");
        r.add("spring.flyway.schemas", () -> "invoice");
    }

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    InvoiceProfileRepository profiles;
    @Autowired
    InvoiceRequestRepository requests;

    static final ProfileData COMPANY = new ProfileData(ProfileType.COMPANY, "某某科技", "91330100XXX",
            "杭州", "0571-000", "招行", "123456");
    static final Instant STAMP = Instant.parse("2026-07-16T00:00:00Z");

    @BeforeEach
    void clean() {
        jdbc.update("DELETE FROM invoice.invoice_pdf");
        jdbc.update("DELETE FROM invoice.invoice_request_order");
        jdbc.update("DELETE FROM invoice.invoice_request");
        jdbc.update("DELETE FROM invoice.invoice_profile");
    }

    static OrderLine order(long orderId, String amount) {
        return new OrderLine(orderId, "T" + orderId, new BigDecimal(amount), Instant.parse("2026-07-01T00:00:00Z"));
    }

    NewRequest newRequest(long userId, long... orderIds) {
        List<OrderLine> lines = java.util.Arrays.stream(orderIds).mapToObj(id -> order(id, "600")).toList();
        BigDecimal total = new BigDecimal(600L * orderIds.length);
        return new NewRequest(userId, total, new BigDecimal("55.00"), COMPANY, STAMP, "备注", lines);
    }

    @Test
    void profileCrudIsScopedByOwner() {
        long id = profiles.create(100, COMPANY, STAMP);
        assertThat(profiles.find(100, id).orElseThrow().data()).isEqualTo(COMPANY);
        assertThat(profiles.find(100, id).orElseThrow().verifiedAt()).isEqualTo(STAMP); // 章原样回读
        assertThat(profiles.find(999, id)).isEmpty();                 // 他人不可见
        assertThat(profiles.update(999, id, COMPANY, null)).isFalse(); // 他人不可改
        assertThat(profiles.update(100, id, COMPANY, null)).isTrue();  // 掉章落库
        assertThat(profiles.find(100, id).orElseThrow().verifiedAt()).isNull();
        assertThat(profiles.countByUser(100)).isEqualTo(1);
        assertThat(profiles.delete(100, id)).isTrue();
        assertThat(profiles.countByUser(100)).isZero();
    }

    @Test
    void createPersistsRequestWithSnapshotsAndInvNo() {
        var created = requests.create(newRequest(100, 9001, 9002));
        assertThat(created.requestNo()).isEqualTo("INV" + created.id());
        assertThat(jdbc.queryForObject("SELECT profile_title FROM invoice.invoice_request WHERE id=?",
                String.class, created.id())).isEqualTo("某某科技");
        assertThat(jdbc.queryForObject("SELECT profile_verified_at IS NOT NULL FROM invoice.invoice_request "
                + "WHERE id=?", Boolean.class, created.id())).isTrue(); // 核验章快照随单落库
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoice.invoice_request_order WHERE request_id=? AND active",
                Long.class, created.id())).isEqualTo(2L);
        assertThat(requests.findState(created.id()).orElseThrow().status()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    void duplicateActiveRequestMapsToDomainException() {
        requests.create(newRequest(100, 9001));
        assertThatThrownBy(() -> requests.create(newRequest(100, 9002)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("进行中");
    }

    @Test
    void occupiedOrderMapsToDomainException() {
        requests.create(newRequest(100, 9001));
        assertThatThrownBy(() -> requests.create(newRequest(200, 9001)))
                .isInstanceOf(InvoiceException.class).hasMessageContaining("占用");
    }

    @Test
    void guardedTransitionsAndRelease() {
        long id = requests.create(newRequest(100, 9001)).id();
        assertThat(requests.markInvoicing(id)).isTrue();
        assertThat(requests.markInvoicing(id)).isFalse();       // 守卫未命中
        assertThat(jdbc.queryForObject("SELECT fee_charged_at IS NOT NULL FROM invoice.invoice_request WHERE id=?",
                Boolean.class, id)).isTrue();
        assertThat(requests.reject(id, "余额不足", Set.of(InvoiceStatus.INVOICING))).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoice.invoice_request_order WHERE request_id=? AND active",
                Long.class, id)).isZero();                       // 驳回释放占用
        // 释放后同订单可再开
        long id2 = requests.create(newRequest(100, 9001)).id();
        assertThat(requests.cancel(id2, 999)).isFalse();         // 他人不可撤
        assertThat(requests.cancel(id2, 100)).isTrue();
        assertThat(requests.findState(id2).orElseThrow().status()).isEqualTo(InvoiceStatus.CANCELLED);
    }

    @Test
    void attachPdfIssuesAndSupportsReupload() throws Exception {
        long id = requests.create(newRequest(100, 9001)).id();
        byte[] pdf = "%PDF-1.4 fake".getBytes();
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(pdf));
        assertThat(requests.attachPdfAndIssue(id, new PdfData("发票.pdf", pdf, sha))).isFalse(); // PENDING 不许传
        requests.markInvoicing(id);
        assertThat(requests.attachPdfAndIssue(id, new PdfData("发票.pdf", pdf, sha))).isTrue();
        assertThat(requests.findState(id).orElseThrow().status()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(jdbc.queryForObject("SELECT sha256 FROM invoice.invoice_pdf WHERE request_id=?", String.class, id))
                .isEqualTo(sha);
        // ISSUED 重传覆盖(传错补救)
        byte[] pdf2 = "%PDF-1.4 v2".getBytes();
        assertThat(requests.attachPdfAndIssue(id, new PdfData("v2.pdf", pdf2, "sha2"))).isTrue();
        assertThat(jdbc.queryForObject("SELECT filename FROM invoice.invoice_pdf WHERE request_id=?", String.class, id))
                .isEqualTo("v2.pdf");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM invoice.invoice_pdf WHERE request_id=?", Long.class, id))
                .isEqualTo(1L);
    }
}
