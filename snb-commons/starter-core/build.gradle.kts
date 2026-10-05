// 核心 starter:JSON 配置、错误解析管道、CommandBus、异步执行器。
plugins {
    id("snb.spring-library")
}

dependencies {
    api(project(":snb-commons:commons-core"))
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-json")
    // 以下三项按 classpath 条件装配,不强加给消费方
    compileOnly("tools.jackson.dataformat:jackson-dataformat-xml")
    compileOnly(libs.resilience4j.circuitbreaker)
    compileOnly("io.micrometer:micrometer-core")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation(project(":snb-commons:starter-test"))
}
