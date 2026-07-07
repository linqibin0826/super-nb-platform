// 启动层:唯一 Spring Boot 应用入口,生成可执行 fat JAR。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.java-base")
    id("org.springframework.boot")
}

val libs = the<VersionCatalogsExtension>().named("libs")

// org.springframework.boot 会重置依赖管理,需重新应用 BOM。
applySnbDependencyManagement(libs)

tasks.bootJar {
    archiveClassifier = ""
}
tasks.jar {
    enabled = false
}

dependencies {
    "testImplementation"(libs.findLibrary("commons-starter-test").get())
}
