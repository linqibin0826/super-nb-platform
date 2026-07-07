package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// generation_image 表 Spring Data 仓储。
public interface GenerationImageJpaRepository extends JpaRepository<GenerationImageEntity, Long> {

    /// 批量取给定生成记录(内部雪花 id)的输出图,按序号升序(列表缩略图回退用)。
    List<GenerationImageEntity> findByGeneration_IdInOrderByIdxAsc(Collection<Long> generationIds);
}
