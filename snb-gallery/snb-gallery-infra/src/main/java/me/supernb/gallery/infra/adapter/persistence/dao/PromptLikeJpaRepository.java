package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.prompt_like 仓储。
public interface PromptLikeJpaRepository extends JpaRepository<PromptLikeEntity, Long> {

    /// 成员行是否已存在(toggle 幂等预检)。
    boolean existsByPromptIdAndUserId(long promptId, long userId);

    /// 批量删除成员行,返回影响行数(并发下 0 行=已被别的请求删过,调用方据此决定是否联动减计数);
    /// 禁用派生 delete——select-then-remove 在并发 0 行删除时会抛 StaleStateException。
    @Modifying
    @Query("DELETE FROM PromptLikeEntity l WHERE l.promptId = :promptId AND l.userId = :userId")
    int deleteMembership(@Param("promptId") long promptId, @Param("userId") long userId);

    /// 取用户在给定条目集合中已点赞的成员行(my-state 批量回填用)。
    List<PromptLikeEntity> findByUserIdAndPromptIdIn(long userId, Collection<Long> promptIds);
}
