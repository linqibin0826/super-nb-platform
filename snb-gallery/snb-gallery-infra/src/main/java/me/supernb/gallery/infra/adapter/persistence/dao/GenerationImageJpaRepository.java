package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// gallery.generation_image 仓储。
public interface GenerationImageJpaRepository extends JpaRepository<GenerationImageEntity, Long> {

    /// 列表页缩略图回退:一次取出整页生成的输出图(按 idx 升序,取首张用)。
    List<GenerationImageEntity> findByGeneration_IdInOrderByIdxAsc(Collection<String> generationIds);
}
