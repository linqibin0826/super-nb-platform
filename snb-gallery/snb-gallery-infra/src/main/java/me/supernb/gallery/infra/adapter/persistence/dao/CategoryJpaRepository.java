package me.supernb.gallery.infra.adapter.persistence.dao;

import java.util.List;
import me.supernb.gallery.infra.adapter.persistence.entity.CategoryEntity;
import me.supernb.gallery.infra.adapter.persistence.entity.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/// gallery.category 仓储。
public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    /// 按类目分组统计已发布条目数,未挂类目的条目不计入。
    @Query("SELECT p.category.id AS id, COUNT(p) AS cnt FROM PromptEntity p "
            + "WHERE p.status = 'published' AND p.category IS NOT NULL GROUP BY p.category.id")
    List<CategoryCountView> publishedCountByCategory();

    /// 类目计数投影(接口投影,一行一类目)。
    interface CategoryCountView {
        /// 类目 id(分组键)。
        Long getId();

        /// 该类目下已发布条目数。
        long getCnt();
    }
}
