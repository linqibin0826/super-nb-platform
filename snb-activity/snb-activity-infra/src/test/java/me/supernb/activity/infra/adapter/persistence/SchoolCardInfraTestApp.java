package me.supernb.activity.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// 包机邀请卡 infra 最小装配:只挂被测 SchoolCardAdapter,
/// JPA/Flyway 走自动装配(家族 TestApp 模式)。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(SchoolCardAdapter.class)
class SchoolCardInfraTestApp {
}
