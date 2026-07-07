package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// prompt_like 表 Spring Data 仓储。
public interface PromptLikeJpaRepository extends JpaRepository<PromptLikeEntity, Long> {

    /// 成员是否已存在(幂等预检)。
    boolean existsByPromptIdAndUserId(long promptId, long userId);

    /// 批量删除成员并返回行数(并发下 0 行=已被删,靠行数定计数增量;
    /// 不能用派生 delete——select-then-remove 并发 0 行会抛 StaleState)。
    @Modifying
    @Query("DELETE FROM PromptLikeEntity l WHERE l.promptId = :promptId AND l.userId = :userId")
    int deleteMembership(@Param("promptId") long promptId, @Param("userId") long userId);

    /// 取用户在给定条目集合中的点赞成员(my-state 批量回填)。
    List<PromptLikeEntity> findByUserIdAndPromptIdIn(long userId, Collection<Long> promptIds);
}
