package me.supernb.content.infra.adapter.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// content infra 层最小测试装配：只挂本上下文的持久化适配器（照 GalleryInfraTestApp）。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({ArticleRepositoryAdapter.class, CategoryRepositoryAdapter.class})
class ContentInfraTestApp {
}
