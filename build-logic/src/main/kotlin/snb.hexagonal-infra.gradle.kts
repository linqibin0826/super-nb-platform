// 基础设施层:JPA 仓储实现、外部适配。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.spring-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // 数据访问走 JdbcTemplate + Flyway(native SQL:advisory lock / FOR UPDATE SKIP LOCKED / RETURNING /
    // FILTER 聚合 / CTE),不引 JPA 实体;spring-boot-starter-jdbc 提供 JdbcTemplate + DataSourceTransactionManager。
    "api"("org.springframework.boot:spring-boot-starter-jdbc")
    "testImplementation"(libs.findLibrary("commons-starter-test").get())
    "testImplementation"(libs.findLibrary("testcontainers-postgresql").get())
    "testImplementation"(libs.findLibrary("testcontainers-junit").get())
}
