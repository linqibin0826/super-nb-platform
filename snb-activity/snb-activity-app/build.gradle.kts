plugins {
    id("snb.hexagonal-app")
}

dependencies {
    api(project(":snb-activity:snb-activity-domain"))

    // UsageBoardCache: ApplicationReadyEvent 启动预热
    implementation("org.springframework.boot:spring-boot")
    // 成就系统停用总闸: 11 个成就 bean @ConditionalOnProperty(activity.achievement.enabled)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    // UsageBoardCache: @Slf4j 编译期 Logger 类型
    implementation("org.slf4j:slf4j-api")
}
