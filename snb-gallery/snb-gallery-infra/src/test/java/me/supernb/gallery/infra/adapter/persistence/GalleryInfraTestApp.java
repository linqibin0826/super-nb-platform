package me.supernb.gallery.infra.adapter.persistence;

import me.supernb.gallery.infra.adapter.persistence.GenerationRepositoryAdapter;
import me.supernb.gallery.infra.adapter.persistence.InteractionRepositoryAdapter;
import me.supernb.gallery.infra.adapter.read.PromptReadAdapter;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

/// gallery infra 最小测试装配:自动配置(JPA/Flyway/JdbcTemplate)+ 三个被测适配器。
/// R2Config 不在装配内(未配 endpoint),存储/缩略图适配器不参与。
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({PromptReadAdapter.class, InteractionRepositoryAdapter.class, GenerationRepositoryAdapter.class})
class GalleryInfraTestApp {
}
