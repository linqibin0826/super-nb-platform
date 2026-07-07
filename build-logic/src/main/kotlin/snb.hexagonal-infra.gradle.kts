// 基础设施层:JPA 仓储实现、外部适配。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.spring-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "api"(libs.findLibrary("commons-starter-jpa").get())
    "testImplementation"(libs.findLibrary("commons-starter-test").get())
    "testImplementation"(libs.findLibrary("testcontainers-postgresql").get())
    "testImplementation"(libs.findLibrary("testcontainers-junit").get())
}
