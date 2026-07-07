// 应用层:用例编排、事务边界。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.spring-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"("org.springframework:spring-tx")
    "implementation"("org.springframework:spring-context")
    "testImplementation"(libs.findLibrary("commons-starter-test").get())
}
