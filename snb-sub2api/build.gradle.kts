plugins {
    id("snb.spring-library")
}

dependencies {
    api(libs.commons.core)
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    testImplementation(libs.commons.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
}
