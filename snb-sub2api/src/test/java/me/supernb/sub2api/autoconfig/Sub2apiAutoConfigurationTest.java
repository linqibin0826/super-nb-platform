package me.supernb.sub2api.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import me.supernb.sub2api.auth.Sub2apiIntrospectClient;
import me.supernb.sub2api.recharge.RechargeReadModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/// starter 条件装配契约:introspect 随 classpath 常开;充值读模型不配 url 就完全不装。
class Sub2apiAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    Sub2apiAutoConfiguration.class, Sub2apiRechargeAutoConfiguration.class));

    @Test
    void introspectAlwaysOnAndReadModelOffWithoutUrl() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(Sub2apiIntrospectClient.class);
            assertThat(ctx).doesNotHaveBean(RechargeReadModel.class);
        });
    }

    @Test
    void readModelOnWhenUrlConfigured() {
        runner.withPropertyValues("sub2api.read-datasource.url=jdbc:postgresql://localhost:5432/sub2api")
                .run(ctx -> assertThat(ctx).hasSingleBean(RechargeReadModel.class));
    }
}
