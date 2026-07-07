// 所有 Java 模块基座:Java 25 toolchain + Spring Boot BOM + Lombok + JUnit5。
// 注意 Gradle 9.5 预编译脚本插件坑:顶层禁用 /** */ 块注释,只用 // 行注释。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    java
    id("io.spring.dependency-management")
}

val libs = the<VersionCatalogsExtension>().named("libs")

group = "me.supernb"
version = "0.1.0-SNAPSHOT"

applySnbDependencyManagement(libs)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
    testLogging { events("passed", "skipped", "failed") }
}

dependencies {
    "compileOnly"(libs.findLibrary("lombok").get())
    "annotationProcessor"(libs.findLibrary("lombok").get())
    "testCompileOnly"(libs.findLibrary("lombok").get())
    "testAnnotationProcessor"(libs.findLibrary("lombok").get())
    "testImplementation"(libs.findLibrary("junit-jupiter").get())
    "testImplementation"(libs.findLibrary("assertj-core").get())
    "testImplementation"(libs.findLibrary("mockito-core").get())
    "testImplementation"(libs.findLibrary("mockito-junit-jupiter").get())
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}
