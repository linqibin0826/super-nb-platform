package me.supernb.activity.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// NB 账本 infra 最小装配:挂账本读适配器与两个记账来源适配器(解锁/打卡),
/// JPA/Flyway 走自动装配(家族 TestApp 模式)。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({NbLedgerAdapter.class, AchievementUnlockAdapter.class, CheckinAdapter.class})
class NbLedgerInfraTestApp {
}
