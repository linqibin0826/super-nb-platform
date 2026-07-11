package me.supernb.content.infra.adapter.persistence.dao;

import me.supernb.content.infra.adapter.persistence.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/// 文章 DAO。
public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, Long> {

    /// 按发布幂等键取文章。
    Optional<ArticleEntity> findBySlug(String slug);

    /// 分类引用计数（整表同步的拒删守卫）。
    long countByCategorySlug(String categorySlug);
}
