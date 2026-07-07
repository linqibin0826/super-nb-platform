plugins {
    `kotlin-dsl`
}

dependencies {
    // Spring Boot Gradle 插件:供 snb.hexagonal-boot 的 id("org.springframework.boot") 使用
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${libs.versions.spring.boot.get()}")
    // Spring dependency-management:供 DependencyManagementExtension(SnbDependencyManagement.kt)使用
    implementation("io.spring.dependency-management:io.spring.dependency-management.gradle.plugin:${libs.versions.spring.dependency.management.get()}")
}
