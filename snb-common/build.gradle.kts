plugins {
    id("snb.spring-library")
}

dependencies {
    api(project(":snb-commons:commons-core"))
    api(project(":snb-commons:starter-web"))
    implementation("org.springframework:spring-webmvc")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
}
