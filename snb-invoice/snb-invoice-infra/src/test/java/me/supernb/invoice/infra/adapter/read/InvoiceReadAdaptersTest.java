package me.supernb.invoice.infra.adapter.read;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.supernb.invoice.domain.model.InvoiceStatus;
import me.supernb.invoice.domain.model.ProfileType;
import me.supernb.invoice.domain.model.read.OrderLine;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository;
import me.supernb.invoice.domain.port.repository.InvoiceProfileRepository.ProfileData;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.NewRequest;
import me.supernb.invoice.domain.port.repository.InvoiceRequestRepository.PdfData;
import me.supernb.invoice.infra.adapter.persistence.InvoiceProfileRepositoryAdapter;
import me.supernb.invoice.infra.adapter.persistence.InvoiceRequestRepositoryAdapter;
import me.supernb.invoice.infra.adapter.persistence.entity.InvoiceProfileEntity;
import me.supernb.invoice.infra.adapter.persistence.dao.InvoiceProfileJpaRepository;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/// 本库读适配器:我的列表/归属单查/占用差集/管理端分页与详情/PDF 只在专用方法加载。
@SpringBootTest(classes = InvoiceReadAdaptersTest.TestApp.class)
@Testcontainers
@Timeout(value = 30, unit = TimeUnit.SECONDS)
class InvoiceReadAdaptersTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackageClasses = InvoiceProfileJpaRepository.class)
    @EntityScan(basePackageClasses = InvoiceProfileEntity.class)
    @Import({InvoiceProfileRepositoryAdapter.class, InvoiceRequestRepositoryAdapter.class,
            InvoiceRequestReadAdapter.class, InvoiceProfileReadAdapter.class})
    static class TestApp {
    }

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
    InvoiceProfileRepository profiles;
    @Autowired
    InvoiceRequestRepository requests;
    @Autowired
    InvoiceRequestReadAdapter requestRead;
    @Autowired
    InvoiceProfileReadAdapter profileRead;

    static final ProfileData PERSONAL = new ProfileData(ProfileType.PERSONAL, "张三", null, null, null, null, null);

    @Test
    void fullReadRoundTrip() {
        long profileId = profiles.create(500, PERSONAL, null);
        assertThat(profileRead.listByUser(500)).singleElement()
                .satisfies(p -> {
                    assertThat(p.id()).isEqualTo(profileId);
                    assertThat(p.title()).isEqualTo("张三");
                    assertThat(p.verifiedAt()).isNull();
                });

        var created = requests.create(new NewRequest(500, new BigDecimal("1200.00"), new BigDecimal("60.00"),
                PERSONAL, null, "七月", List.of(
                        new OrderLine(8001, "T8001", new BigDecimal("700"), Instant.parse("2026-07-01T00:00:00Z")),
                        new OrderLine(8002, "T8002", new BigDecimal("500"), Instant.parse("2026-07-02T00:00:00Z")))));

        assertThat(requestRead.listByUser(500)).singleElement().satisfies(v -> {
            assertThat(v.requestNo()).isEqualTo(created.requestNo());
            assertThat(v.status()).isEqualTo(InvoiceStatus.PENDING);
            assertThat(v.fee()).isEqualByComparingTo("60.00");
        });
        assertThat(requestRead.findMine(999, created.id())).isEmpty();   // 他人不可见
        assertThat(requestRead.occupiedOrderIds(500)).isEqualTo(Set.of(8001L, 8002L));

        var page = requestRead.pageByStatus(InvoiceStatus.PENDING, 1, 10);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.items().get(0).userId()).isEqualTo(500);
        assertThat(requestRead.pageByStatus(null, 1, 10).total()).isEqualTo(1);   // 全部
        assertThat(requestRead.pageByStatus(InvoiceStatus.ISSUED, 1, 10).total()).isZero();

        var detail = requestRead.findDetail(created.id()).orElseThrow();
        assertThat(detail.orders()).hasSize(2);
        assertThat(detail.profileTitle()).isEqualTo("张三");

        assertThat(requestRead.pdfOf(created.id())).isEmpty();
        requests.markInvoicing(created.id());
        requests.attachPdfAndIssue(created.id(), new PdfData("发票.pdf", "%PDF-1.4 x".getBytes(), "s"));
        var pdf = requestRead.pdfOf(created.id()).orElseThrow();
        assertThat(pdf.filename()).isEqualTo("发票.pdf");
        assertThat(new String(pdf.bytes())).startsWith("%PDF");

        // 撤回/驳回后释放占用,差集回空
        requests.reject(created.id(), "test", Set.of(InvoiceStatus.INVOICING, InvoiceStatus.ISSUED,
                InvoiceStatus.PENDING));
        assertThat(requestRead.occupiedOrderIds(500)).isEmpty();
    }
}
