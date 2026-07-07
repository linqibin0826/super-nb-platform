// 适配器层:REST 控制器。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.spring-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"("org.springframework:spring-webmvc")
    "compileOnly"("jakarta.servlet:jakarta.servlet-api")
    "testImplementation"(libs.findLibrary("commons-starter-test").get())
}
