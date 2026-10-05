// JPA starter:实体基类、雪花 ID、审计、Hibernate 调优、数据层错误映射、Flyway + PG 驱动。
plugins {
    id("snb.spring-library")
}

dependencies {
    api(project(":snb-commons:commons-core"))
    api(project(":snb-commons:starter-core"))
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("tools.jackson.core:jackson-databind")
    api("org.springframework.boot:spring-boot-starter-flyway")
    api("org.flywaydb:flyway-database-postgresql")
    api("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation(project(":snb-commons:starter-test"))
}
