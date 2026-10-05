// 通用核心库(纯 Java):CQRS 接口、DDD 基类、异常体系、JSON 工具、分页。
plugins {
    id("snb.java-library")
}

dependencies {
    api(libs.hutool.core)
    api("tools.jackson.core:jackson-core")
    api("tools.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("org.slf4j:slf4j-api")
    testRuntimeOnly("org.slf4j:slf4j-api")
}
