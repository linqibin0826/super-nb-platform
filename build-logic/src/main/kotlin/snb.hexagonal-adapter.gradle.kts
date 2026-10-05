// 适配器层:REST 控制器。
plugins {
    id("snb.spring-library")
}

dependencies {
    "implementation"("org.springframework:spring-webmvc")
    "compileOnly"("jakarta.servlet:jakarta.servlet-api")
    "testImplementation"(project(":snb-commons:starter-test"))
}
