package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// generation 表 Spring Data 仓储(身份=雪花 id)。
public interface GenerationJpaRepository extends JpaRepository<GenerationEntity, Long> {

    /// 按 id + 归属用户定位生成记录。
    Optional<GenerationEntity> findByIdAndUserId(long id, long userId);

    /// 按 id + 归属用户定位并预取输出图。
    @Query("SELECT g FROM GenerationEntity g LEFT JOIN FETCH g.images "
            + "WHERE g.id = :id AND g.userId = :userId")
    Optional<GenerationEntity> findWithImages(@Param("id") long id, @Param("userId") long userId);

    /// 用户生成历史分页。
    Page<GenerationEntity> findByUserId(long userId, Pageable pageable);
}
