plugins {
    id("snb.spring-library")
}

dependencies {
    api(libs.commons.core)
    // @CurrentUser 解析器抛共用的 UnauthorizedException(401 语义全平台一份)
    implementation(project(":snb-common"))
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // web 集成(WebMvcConfigurer)按 classpath 条件装配,不强加给非 web 消费方
    compileOnly("org.springframework:spring-webmvc")
    testImplementation(libs.commons.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation("org.springframework:spring-webmvc")
}
