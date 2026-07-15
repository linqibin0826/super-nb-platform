package me.supernb.invoice.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// invoice infra 层最小测试装配:只挂本上下文持久化适配器(照 ContentInfraTestApp)。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({InvoiceProfileRepositoryAdapter.class, InvoiceRequestRepositoryAdapter.class})
class InvoiceInfraTestApp {
}
