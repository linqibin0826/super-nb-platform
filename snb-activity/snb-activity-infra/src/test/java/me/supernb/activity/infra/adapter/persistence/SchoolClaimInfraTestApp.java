package me.supernb.activity.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// 开学季领取台账 infra 最小装配:只挂被测 SchoolClaimAdapter,
/// JPA/Flyway 走自动装配(家族 TestApp 模式)。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(SchoolClaimAdapter.class)
class SchoolClaimInfraTestApp {
}
