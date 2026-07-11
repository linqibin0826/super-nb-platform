package me.supernb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/// 全局开启 @Scheduled 定时任务(用量榜缓存刷新/名次快照是首个消费方)。
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
