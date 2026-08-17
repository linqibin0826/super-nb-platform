package me.supernb.ops.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// ops infra 层最小测试装配:只挂本上下文持久化适配器(照 InvoiceInfraTestApp)。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({OpsAccountRepositoryAdapter.class, OpsSubscriptionRepositoryAdapter.class})
class OpsInfraTestApp {
}
