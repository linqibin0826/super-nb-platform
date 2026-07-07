package me.supernb.gallery.infra.jpa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.generation_ref 仓储。
public interface GenerationRefJpaRepository extends JpaRepository<GenerationRefEntity, GenerationRefId> {

    /// 生成引用的参考图 R2 键(按引用位次序):关联该用户去重库取键。
    @Query("SELECT ri.r2Key FROM GenerationRefEntity gr, RefImageEntity ri "
            + "WHERE gr.id.generationId = :generationId AND ri.id.userId = :userId "
            + "AND ri.id.sha256 = gr.sha256 ORDER BY gr.id.idx")
    List<String> refKeysOf(@Param("generationId") String generationId, @Param("userId") long userId);
}
