// 测试 starter:测试框架全家桶、Testcontainers 容器工厂、ArchUnit 架构规则。
plugins {
    id("snb.spring-library")
}

dependencies {
    api(libs.junit.jupiter)
    api(libs.assertj.core)
    api(libs.mockito.core)
    api(libs.mockito.junit.jupiter)
    api("org.springframework.boot:spring-boot-starter-test") {
        // 测试框架用上面统一管理的版本
        exclude(group = "org.junit.jupiter", module = "junit-jupiter")
        exclude(group = "org.mockito", module = "mockito-core")
        exclude(group = "org.mockito", module = "mockito-junit-jupiter")
        exclude(group = "org.assertj", module = "assertj-core")
    }
    api("org.springframework.boot:spring-boot-starter-data-jpa-test")
    api("org.springframework.boot:spring-boot-starter-jdbc-test")
    api("org.springframework.boot:spring-boot-starter-flyway-test")
    api("org.flywaydb:flyway-database-postgresql")
    api("org.springframework.boot:spring-boot-starter-webmvc-test")
    api("org.springframework.boot:spring-boot-starter-restclient-test")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework:spring-tx")
    api(libs.testcontainers.core)
    api(libs.testcontainers.junit)
    api(libs.testcontainers.postgresql)
    api(libs.testcontainers.minio)
    api(libs.minio) {
        exclude(group = "org.jetbrains", module = "annotations")
    }
    api(libs.archunit.junit5)
    api(libs.wiremock.standalone)
    api(libs.awaitility)
    api("io.micrometer:micrometer-core")
    api("org.postgresql:postgresql")
    api("org.slf4j:slf4j-api")
    compileOnly("tools.jackson.core:jackson-databind")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

// src/test 下只有给 ArchUnit 规则当样本的类,没有测试用例
tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}
