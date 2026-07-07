plugins {
    id("snb.spring-library")
}

dependencies {
    api(libs.commons.core)
    api(libs.commons.starter.web)
    implementation("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
