plugins {
    id("snb.hexagonal-infra")
}

dependencies {
    api(project(":snb-invoice:snb-invoice-app"))
    implementation(project(":snb-sub2api"))
    // 抬头核验出站 HTTP 客户端(RestClient/JdkClientHttpRequestFactory 在 spring-web)
    implementation("org.springframework:spring-web")
}
