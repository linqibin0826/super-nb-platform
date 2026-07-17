package me.supernb.sub2api.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/// 回归网:autoconfig 包里每个 @AutoConfiguration 类都必须登记进 imports 文件。
/// Sub2apiChatAutoConfiguration 曾漏登记——类和条件全对但 Spring 压根不看,
/// 生产报「AI 识别通道未配置」且环境变量三层齐全,极难定位(2026-07-17)。
/// 用类文件清单+注解判定,不走条件求值(否则 @ConditionalOnProperty 缺属性会把漏网类滤掉)。
class AutoConfigurationImportsTest {

    @Test
    void everyAutoConfigurationIsRegisteredInImports() throws Exception {
        List<String> registered = new String(new ClassPathResource(
                "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .lines().map(String::strip).filter(s -> !s.isEmpty()).toList();

        // classpath*: 扫全部根——本测试类与被扫包同包名,单根 classpath: 会先命中 test 根空手而归
        var resolver = new PathMatchingResourcePatternResolver();
        List<String> annotated = Arrays.stream(
                        resolver.getResources("classpath*:me/supernb/sub2api/autoconfig/*AutoConfiguration.class"))
                .map(r -> "me.supernb.sub2api.autoconfig."
                        + Objects.requireNonNull(r.getFilename()).replace(".class", ""))
                .filter(name -> {
                    try {
                        return Class.forName(name).isAnnotationPresent(AutoConfiguration.class);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .sorted().toList();

        assertThat(annotated).isNotEmpty();
        assertThat(registered).containsAll(annotated);
    }
}
