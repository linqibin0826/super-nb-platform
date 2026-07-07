package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.generation 仓储。
public interface GenerationJpaRepository extends JpaRepository<GenerationEntity, String> {

    Optional<GenerationEntity> findByIdAndUserId(String id, long userId);

    /// 详情/删除用:连输出图一并取出(单行 fetch join,分页无关)。
    @Query("SELECT g FROM GenerationEntity g LEFT JOIN FETCH g.images WHERE g.id = :id AND g.userId = :userId")
    Optional<GenerationEntity> findWithImages(@Param("id") String id, @Param("userId") long userId);

    Page<GenerationEntity> findByUserId(long userId, Pageable pageable);
}
