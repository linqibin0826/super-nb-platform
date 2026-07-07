package me.supernb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// snb-platform 单体应用入口,全仓库唯一 `@SpringBootApplication`。默认扫描以自身
/// 所在包为根,天然覆盖 `me.supernb` 下 activity/gallery 各上下文的全部组件,
/// 不需要额外声明扫描路径。
@SpringBootApplication
public class SnbPlatformApplication {

    /// 启动入口:交给 SpringApplication 完成自动配置扫描与容器启动。
    public static void main(String[] args) {
        SpringApplication.run(SnbPlatformApplication.class, args);
    }
}
