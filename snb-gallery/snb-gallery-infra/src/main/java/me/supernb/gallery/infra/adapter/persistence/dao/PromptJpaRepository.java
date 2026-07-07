package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.Optional;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/// gallery.prompt 仓储:详情读取 + 反规范化计数列(like_count/fav_count)原子增减。
public interface PromptJpaRepository extends JpaRepository<PromptEntity, Long> {

    /// 条目是否存在且处于给定状态;互动前置校验用,未发布条目不接受点赞/收藏。
    boolean existsByIdAndStatus(Long id, String status);

    /// 按 id 取已发布条目,并 left join fetch 类目,详情页一次查询带出类目,避免懒加载 N+1。
    @Query("SELECT p FROM PromptEntity p LEFT JOIN FETCH p.category WHERE p.id = :id AND p.status = 'published'")
    Optional<PromptEntity> findPublishedWithCategory(@Param("id") long id);

    /// 点赞计数原子加减(delta 正负两用),返回影响行数;禁止读-改-写,避免并发丢更新。
    @Modifying
    @Query("UPDATE PromptEntity p SET p.likeCount = p.likeCount + :delta WHERE p.id = :id")
    int adjustLikeCount(@Param("id") long id, @Param("delta") int delta);

    /// 收藏计数原子加减,语义同 adjustLikeCount。
    @Modifying
    @Query("UPDATE PromptEntity p SET p.favCount = p.favCount + :delta WHERE p.id = :id")
    int adjustFavCount(@Param("id") long id, @Param("delta") int delta);

    /// 取当前点赞数;toggle 完成后或撞唯一约束回退时,用于回读最新值返回给调用方。
    @Query("SELECT p.likeCount FROM PromptEntity p WHERE p.id = :id")
    Optional<Integer> likeCountOf(@Param("id") long id);

    /// 取当前收藏数,语义同 likeCountOf。
    @Query("SELECT p.favCount FROM PromptEntity p WHERE p.id = :id")
    Optional<Integer> favCountOf(@Param("id") long id);
}
