// 领域层:纯 Java,禁止框架依赖(enforceDomainPurity 编译期强制)。
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("snb.java-library")
}

val libs = the<VersionCatalogsExtension>().named("libs")

val bannedGroups = setOf(
    "org.springframework",
    "org.springframework.boot",
    "org.springframework.data",
    "jakarta.persistence",
    "jakarta.validation",
    "org.hibernate",
    "org.hibernate.orm",
    "org.apache.tomcat",
    "io.netty"
)

abstract class DomainPurityCheck : DefaultTask() {
    @get:Input
    abstract val violations: ListProperty<String>

    @TaskAction
    fun check() {
        val v = violations.get()
        if (v.isNotEmpty()) {
            throw GradleException("domain 层禁止框架依赖: ${v.joinToString(", ")}")
        }
    }
}

tasks.register<DomainPurityCheck>("enforceDomainPurity") {
    group = "verification"
    description = "校验领域层无框架依赖"
    violations.set(provider {
        val result = mutableListOf<String>()
        configurations.filter { it.name in listOf("compileClasspath", "runtimeClasspath") }
            .forEach { config ->
                config.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                    val id = artifact.moduleVersion.id
                    if (bannedGroups.any { id.group.startsWith(it) }) {
                        result.add("${id.group}:${id.name}")
                    }
                }
            }
        result
    })
}

tasks.named("check") {
    dependsOn("enforceDomainPurity")
}

dependencies {
    "api"(libs.findLibrary("commons-core").get())
}
