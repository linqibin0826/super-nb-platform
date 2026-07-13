package me.supernb.activity.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/// 纯 schema 冒烟测试装配:不 @Import 任何被测 Bean,只要 JPA/Flyway 自动装配跑起来。
@SpringBootConfiguration
@EnableAutoConfiguration
class CheckinSchemaMigrationTestApp {
}
