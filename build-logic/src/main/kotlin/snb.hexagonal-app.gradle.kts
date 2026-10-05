// 应用层:用例编排、事务边界。
plugins {
    id("snb.spring-library")
}

dependencies {
    "implementation"("org.springframework:spring-tx")
    "implementation"("org.springframework:spring-context")
    "testImplementation"(project(":snb-commons:starter-test"))
}
