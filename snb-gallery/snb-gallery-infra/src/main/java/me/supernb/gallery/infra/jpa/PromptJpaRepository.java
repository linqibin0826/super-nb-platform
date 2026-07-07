package me.supernb.gallery.infra.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.prompt 仓储:详情读取 + 反规范化计数列增减。
public interface PromptJpaRepository extends JpaRepository<PromptEntity, Long> {

    boolean existsByIdAndStatus(Long id, String status);

    @Query("SELECT p FROM PromptEntity p LEFT JOIN FETCH p.category WHERE p.id = :id AND p.status = 'published'")
    Optional<PromptEntity> findPublishedWithCategory(@Param("id") long id);

    @Modifying
    @Query("UPDATE PromptEntity p SET p.likeCount = p.likeCount + :delta WHERE p.id = :id")
    int adjustLikeCount(@Param("id") long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE PromptEntity p SET p.favCount = p.favCount + :delta WHERE p.id = :id")
    int adjustFavCount(@Param("id") long id, @Param("delta") int delta);

    @Query("SELECT p.likeCount FROM PromptEntity p WHERE p.id = :id")
    Optional<Integer> likeCountOf(@Param("id") long id);

    @Query("SELECT p.favCount FROM PromptEntity p WHERE p.id = :id")
    Optional<Integer> favCountOf(@Param("id") long id);
}
