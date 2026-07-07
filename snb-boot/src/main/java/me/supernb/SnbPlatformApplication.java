package me.supernb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/// snb-platform 单体应用入口。扫描 me.supernb 下全部上下文组件。
@SpringBootApplication
public class SnbPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnbPlatformApplication.class, args);
    }
}
