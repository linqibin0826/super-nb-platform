package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationRefEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.RefImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// generation_ref 表 Spring Data 仓储。
public interface GenerationRefJpaRepository extends JpaRepository<GenerationRefEntity, Long> {

    /// 取某生成记录(内部雪花 id)的参考图 R2 键,按参考序号升序
    /// (经 (user_id, sha256) 内容寻址联 ref_image)。
    @Query("SELECT ri.r2Key FROM GenerationRefEntity gr, RefImageEntity ri "
            + "WHERE gr.generation.id = :generationId AND ri.userId = :userId "
            + "AND ri.sha256 = gr.sha256 ORDER BY gr.idx")
    List<String> refKeysOf(@Param("generationId") long generationId, @Param("userId") long userId);
}
