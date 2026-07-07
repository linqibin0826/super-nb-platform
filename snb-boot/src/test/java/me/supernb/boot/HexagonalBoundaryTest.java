package me.supernb.boot;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import dev.linqibin.commons.cqrs.CommandHandler;

/// 六边形依赖边界(编译产物级)。domain 纯净、app 不反向依赖 infra/adapter。
@AnalyzeClasses(packages = "me.supernb", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalBoundaryTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFreeAndInward = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..app..", "..infra..", "..adapter..",
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..");

    @ArchTest
    static final ArchRule appDoesNotDependOnInfraOrAdapter = noClasses()
            .that().resideInAPackage("..app..")
            .should().dependOnClassesThat().resideInAnyPackage("..infra..", "..adapter..");

    @ArchTest
    static final ArchRule appIsPersistenceFree = noClasses()
            .that().resideInAPackage("..app..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..")
            .as("app 端口层不感知持久化技术(JPA 实体/仓储只在 infra)");

    @ArchTest
    static final ArchRule domainDoesNotDependOnOtherContexts = noClasses()
            .that().resideInAPackage("..activity..")
            .should().dependOnClassesThat().resideInAPackage("..gallery..")
            .as("上下文之间不互相依赖(activity 不依赖 gallery)");

    @ArchTest
    static final ArchRule aclStaysOutOfDomainAndApp = noClasses()
            .that().resideInAnyPackage("..domain..", "..app..")
            .should().dependOnClassesThat().resideInAPackage("me.supernb.sub2api..")
            .as("sub2api 防腐层类型只进 infra/adapter,不进 domain/app(各上下文用自己的端口适配)");

    @ArchTest
    static final ArchRule adapterInjectsBusNotHandlers = noClasses()
            .that().resideInAPackage("..adapter..")
            .should().dependOnClassesThat().implement(CommandHandler.class)
            .as("adapter 写路径只注入 CommandBus,禁止直接依赖 CommandHandler 实现");

    @ArchTest
    static final ArchRule apiIsContractOnly = noClasses()
            .that().resideInAPackage("me.supernb..api..")
            .should().dependOnClassesThat().resideInAnyPackage("..domain..", "..app..", "..infra..", "..adapter..")
            .as("api 契约模块不依赖任何实现层(domain/app/infra/adapter)");
}
