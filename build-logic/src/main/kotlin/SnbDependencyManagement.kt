import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.configure

// 应用 Spring Boot BOM,使各模块可声明无版本号的 spring/jakarta/flyway 等依赖。
// 用 extensions.configure 而非 dependencyManagement{} DSL:后者的类型访问器在预编译脚本插件里不生成。
fun Project.applySnbDependencyManagement(libs: VersionCatalog) {
    val springBootVersion = libs.findVersion("spring-boot").get().requiredVersion
    extensions.configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }
}
