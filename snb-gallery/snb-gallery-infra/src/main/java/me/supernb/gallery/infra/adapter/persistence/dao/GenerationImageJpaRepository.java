package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/// gallery.generation_image 仓储。
public interface GenerationImageJpaRepository extends JpaRepository<GenerationImageEntity, Long> {

    /// 批量取给定生成记录(雪花 id)集合的输出图,按序号(idx)升序;
    /// 供列表分页的缩略图回退批量取首图键,避免逐行 N+1。
    List<GenerationImageEntity> findByGeneration_IdInOrderByIdxAsc(Collection<Long> generationIds);
}
