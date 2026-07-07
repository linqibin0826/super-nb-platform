package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Collection;
import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.InteractionId;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptFavoriteEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.prompt_favorite 仓储。
public interface PromptFavoriteJpaRepository extends JpaRepository<PromptFavoriteEntity, InteractionId> {

    /// 批量删除式取消收藏:返回真实删除行数(0 = 本来就没收藏)。
    @Modifying
    @Query("DELETE FROM PromptFavoriteEntity f WHERE f.id.promptId = :promptId AND f.id.userId = :userId")
    int deleteMembership(@Param("promptId") long promptId, @Param("userId") long userId);

    List<PromptFavoriteEntity> findById_UserIdAndId_PromptIdIn(long userId, Collection<Long> promptIds);

    /// 我的收藏(只出 published,按收藏时间倒序)。
    @Query(value = "SELECT p FROM PromptFavoriteEntity f, PromptEntity p "
            + "WHERE p.id = f.id.promptId AND f.id.userId = :userId AND p.status = 'published' "
            + "ORDER BY f.createdAt DESC, f.id.promptId DESC",
            countQuery = "SELECT COUNT(f) FROM PromptFavoriteEntity f, PromptEntity p "
            + "WHERE p.id = f.id.promptId AND f.id.userId = :userId AND p.status = 'published'")
    Page<PromptEntity> favoritePrompts(@Param("userId") long userId, Pageable pageable);
}
