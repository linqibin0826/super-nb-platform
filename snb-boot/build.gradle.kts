plugins {
    id("snb.hexagonal-boot")
}

springBoot {
    mainClass = "me.supernb.SnbPlatformApplication"
}

dependencies {
    implementation(project(":snb-activity:snb-activity-adapter"))
    implementation(project(":snb-activity:snb-activity-infra"))
    implementation(project(":snb-activity:snb-activity-api"))
    implementation(project(":snb-gallery:snb-gallery-adapter"))
    implementation(project(":snb-gallery:snb-gallery-infra"))
    implementation(project(":snb-gallery:snb-gallery-api"))
    implementation(project(":snb-common"))
    implementation(project(":snb-sub2api"))

    implementation(libs.commons.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-webmvc-test")
    testImplementation(libs.commons.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.archunit.junit5)
}
