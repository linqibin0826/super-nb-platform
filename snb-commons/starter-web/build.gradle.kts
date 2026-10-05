// Web starter:Spring MVC、Jackson、参数校验、RFC 7807 统一错误响应。
plugins {
    id("snb.spring-library")
}

dependencies {
    api(project(":snb-commons:commons-core"))
    api(project(":snb-commons:starter-core"))
    api("org.springframework.boot:spring-boot-starter-webmvc")
    api("org.springframework.boot:spring-boot-starter-jackson")
    api("org.springframework.boot:spring-boot-starter-validation")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation(project(":snb-commons:starter-test"))
}
