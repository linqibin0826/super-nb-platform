package me.supernb.gallery.infra.jpa;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.prompt_like 仓储。
public interface PromptLikeJpaRepository extends JpaRepository<PromptLikeEntity, InteractionId> {

    /// 批量删除式退赞:返回真实删除行数(0 = 本来就没赞),并发下由行锁保证只有一方删中。
    @Modifying
    @Query("DELETE FROM PromptLikeEntity l WHERE l.id.promptId = :promptId AND l.id.userId = :userId")
    int deleteMembership(@Param("promptId") long promptId, @Param("userId") long userId);

    List<PromptLikeEntity> findById_UserIdAndId_PromptIdIn(long userId, Collection<Long> promptIds);
}
