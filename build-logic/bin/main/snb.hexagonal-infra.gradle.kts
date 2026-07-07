// 基础设施层:JPA 实体/仓储实现、外部适配。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.spring-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    // 数据访问走 commons JPA starter(Spring Data JPA + Hibernate 7 调优/审计/错误映射,
    // 附带 Flyway + PG 驱动);PG 特有语句(advisory lock / FOR UPDATE SKIP LOCKED)保留 nativeQuery。
    "api"(libs.findLibrary("commons-starter-jpa").get())
    "testImplementation"(libs.findLibrary("commons-starter-test").get())
    "testImplementation"(libs.findLibrary("testcontainers-postgresql").get())
    "testImplementation"(libs.findLibrary("testcontainers-junit").get())
}
