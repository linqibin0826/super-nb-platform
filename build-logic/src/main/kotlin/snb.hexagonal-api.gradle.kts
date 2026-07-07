// API 层:上下文对外契约(DTO/接口/常量),无业务逻辑。
// 单体形态下即「跨上下文调用契约」——被调方契约放自己的 api,消费方 infra 薄适配。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.java-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "api"(libs.findLibrary("commons-core").get())
    // DTO 校验注解(仅注解,不拉实现)
    "compileOnly"("jakarta.validation:jakarta.validation-api")
    // HTTP Interface 契约注解(@HttpExchange 等,provided)
    "compileOnly"("org.springframework:spring-web")
}

// 契约模块通常没有测试(纯数据结构)
tasks.named<Test>("test") {
    enabled = false
}
