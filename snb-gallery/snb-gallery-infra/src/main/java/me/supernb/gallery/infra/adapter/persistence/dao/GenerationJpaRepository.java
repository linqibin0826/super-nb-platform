package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.gallery.infra.adapter.persistence.entity.GenerationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// generation 表 Spring Data 仓储。对外定位一律走 client_task_id(前端任务 uuid),
/// 雪花主键仅内部关联用。
public interface GenerationJpaRepository extends JpaRepository<GenerationEntity, Long> {

    /// 按任务 uuid + 归属用户定位生成记录。
    Optional<GenerationEntity> findByClientTaskIdAndUserId(String clientTaskId, long userId);

    /// 按任务 uuid + 归属用户定位并预取输出图。
    @Query("SELECT g FROM GenerationEntity g LEFT JOIN FETCH g.images "
            + "WHERE g.clientTaskId = :clientTaskId AND g.userId = :userId")
    Optional<GenerationEntity> findWithImages(@Param("clientTaskId") String clientTaskId,
                                              @Param("userId") long userId);

    /// 用户生成历史分页。
    Page<GenerationEntity> findByUserId(long userId, Pageable pageable);
}
