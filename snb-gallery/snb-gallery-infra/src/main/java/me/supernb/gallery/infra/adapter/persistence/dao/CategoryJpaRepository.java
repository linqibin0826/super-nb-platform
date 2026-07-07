package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.CategoryEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/// category 表 Spring Data 仓储。
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    /// 按类目统计已发布条目数(只统计挂了类目的条目)。
    @Query("SELECT p.category.id AS id, COUNT(p) AS cnt FROM PromptEntity p "
            + "WHERE p.status = 'published' AND p.category IS NOT NULL GROUP BY p.category.id")
    List<CategoryCountView> publishedCountByCategory();

    /// 类目计数投影。
    interface CategoryCountView {
        /// 类目 id。
        Long getId();

        /// 已发布条目数。
        long getCnt();
    }
}
