package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptFavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// prompt_favorite 表 Spring Data 仓储。
public interface PromptFavoriteJpaRepository extends JpaRepository<PromptFavoriteEntity, Long> {

    /// 成员是否已存在(幂等预检)。
    boolean existsByPromptIdAndUserId(long promptId, long userId);

    /// 批量删除成员并返回行数(并发下 0 行=已被删,靠行数定计数增量)。
    @Modifying
    @Query("DELETE FROM PromptFavoriteEntity f WHERE f.promptId = :promptId AND f.userId = :userId")
    int deleteMembership(@Param("promptId") long promptId, @Param("userId") long userId);

    /// 取用户在给定条目集合中的收藏成员(my-state 批量回填)。
    List<PromptFavoriteEntity> findByUserIdAndPromptIdIn(long userId, Collection<Long> promptIds);

    /// 「我的收藏」分页:按收藏时刻倒序取仍在发布中的条目。
    @Query(value = "SELECT p FROM PromptFavoriteEntity f, PromptEntity p "
            + "WHERE p.id = f.promptId AND f.userId = :userId AND p.status = 'published' "
            + "ORDER BY f.createdAt DESC, f.promptId DESC",
            countQuery = "SELECT COUNT(f) FROM PromptFavoriteEntity f, PromptEntity p "
            + "WHERE p.id = f.promptId AND f.userId = :userId AND p.status = 'published'")
    Page<PromptEntity> favoritePrompts(@Param("userId") long userId, Pageable pageable);
}
